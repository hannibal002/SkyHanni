package at.hannibal2.skyhanni.features.combat.end.golem

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ElectionApi.derpy
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.entity.EntityDeathEvent
import at.hannibal2.skyhanni.events.entity.EntityLeaveWorldEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.EntityUtils.getBlockInHand
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesNearby
import at.hannibal2.skyhanni.utils.MobUtils
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Blocks
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * Counts the zealots the player killed on the current server, for [GolemDisplay]. Zealot kills add
 * up to 100 to the protector weight, so the number shows how much of that is already secured.
 *
 * The exact source is the bestiary widget in the tab list: Hypixel only counts the player's own kills
 * there, whatever weapon landed them, and Zealots and Zealot Bruisers share the one entry. The widget
 * can be turned off, though, and shows no number any more once the bestiary is maxed.
 *
 * Only then are kills taken from the player's own input, because the client is never told who
 * damaged a mob: a zealot counts when it dies shortly after a melee hit or a click aimed at it. That
 * misses zealots only caught in the splash of an area ability.
 */
@SkyHanniModule
object GolemZealotCounter {

    private val patternGroup = RepoPattern.group("combat.end-zealot-counter")

    /**
     * WRAPPED-REGEX-TEST: " Zealot 38: 8,613/10,000"
     * WRAPPED-REGEX-TEST: " Zealot XXXVIII: 8,613/10,000"
     */
    private val bestiaryPattern by patternGroup.pattern(
        "tablist.bestiary",
        "\\s*Zealot (?:\\d+|[IVXLC]+): (?<kills>[\\d,.]+)/[\\d,.]+",
    )

    /**
     * A bigger jump between two widget updates is the widget itself jumping rather than a run of
     * kills, and is left out.
     */
    private const val MAX_KILLS_PER_UPDATE = 50L

    /** How far away a zealot may be for a click aimed at it to count as a hit. */
    private const val AIM_RANGE = 50.0

    /** How far above a zealot its nametag may sit while the mob detection has not paired them yet. */
    private const val NAMETAG_RADIUS = 3.0

    /** Base health of Zealots and Zealot Bruisers, runic ones have four times as much. */
    private val zealotHealth = setOf(13_000, 65_000, 13_000 * 4, 65_000 * 4)

    /**
     * How long a hit stays attributed to the player. Without the limit, a zealot the player once aimed
     * at and another player finished off much later would still be counted.
     */
    private val HIT_MEMORY = 5.seconds

    /** Kills the bestiary widget showed last, null while it shows no number for zealots. */
    private var bestiaryKills: Long? = null

    /** While the bestiary provides the kills, the hit detection stays out so nothing counts twice. */
    private val countsFromBestiary get() = bestiaryKills != null

    /** The widget is the only exact source, so the display points out when it is turned off. */
    val bestiaryWidgetMissing get() = !TabWidget.BESTIARY.isActive

    /** Zealots the player hit, with the time of the latest hit. */
    private val hitZealots = mutableMapOf<UUID, SimpleTimeMark>()

    /** Zealots killed on this server. */
    var kills = 0
        private set

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.BESTIARY)) return
        val newKills = event.cleanLines.firstNotNullOfOrNull { line ->
            bestiaryPattern.matchMatcher(line) { group("kills").formatLong() }
        }
        val previousKills = bestiaryKills
        bestiaryKills = newKills
        if ((newKills == null) != (previousKills == null)) {
            ChatUtils.consoleLog("[GolemZealotCounter] bestiary kills: ${newKills ?: "not shown"}")
        }
        if (newKills == null || previousKills == null) return
        // Anything outside this range - a lower number included - only moves the baseline.
        val difference = newKills - previousKills
        if (difference in 1..MAX_KILLS_PER_UPDATE) kills += difference.toInt()
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onEntityClick(event: EntityClickEvent) {
        if (countsFromBestiary || event.action != EntityClickEvent.ActionType.ATTACK) return
        // A click on the nametag hits the zealot it belongs to.
        registerHit(event.clickedEntity.mob?.baseEntity ?: event.clickedEntity)
    }

    /** Every left and right click, including those that hit nothing within melee range. */
    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onItemClick(event: ItemClickEvent) {
        if (countsFromBestiary) return
        val player = MinecraftCompat.localPlayerOrNull ?: return
        // Players are skipped, the own one included: the ray starts inside its hitbox.
        val target = MobUtils.raycastForMobs(player, AIM_RANGE, partialTicks = 1f)
            ?.firstOrNull { it.baseEntity !is Player } ?: return
        registerHit(target.baseEntity)
    }

    private fun registerHit(entity: Entity) {
        if (!isZealot(entity)) return
        if (hitZealots.put(entity.uuid, SimpleTimeMark.now()) == null) {
            ChatUtils.consoleLog("[GolemZealotCounter] hit ${entity.id}")
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onEntityDeath(event: EntityDeathEvent<EnderMan>) {
        if (countsFromBestiary) return
        val entity = event.entity
        ChatUtils.consoleLog(
            "[GolemZealotCounter] death ${entity.id}: zealot=${isZealot(entity)}, hit=${entity.uuid in hitZealots}, " +
                "health=${entity.baseMaxHealth}, mob=${entity.mob?.name}",
        )
        countKill(entity)
    }

    /**
     * Backup for a death that only shows as the health dropping to zero and never reaches
     * [onEntityDeath]. A zealot leaving alive - out of range, say - is simply forgotten.
     */
    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onEntityLeaveWorld(event: EntityLeaveWorldEvent<EnderMan>) {
        if (event.entity.isDeadOrDying) countKill(event.entity)
        else hitZealots.remove(event.entity.uuid)
    }

    private fun countKill(entity: Entity) {
        val lastHit = hitZealots.remove(entity.uuid) ?: return
        if (countsFromBestiary || lastHit.passedSince() > HIT_MEMORY) return
        kills++
    }

    /**
     * Zealot, Zealot Bruiser and Special Zealot all carry "Zealot" in their name, but the mob detection
     * and the nametag both take a moment after a zealot appears. The base health and the portal frame a
     * special zealot holds are known right away.
     */
    private fun isZealot(entity: Entity): Boolean {
        if (entity !is EnderMan) return false
        if (entity.mob?.name?.contains("Zealot") == true) return true
        if (entity.getBlockInHand()?.block == Blocks.END_PORTAL_FRAME) return true
        val health = entity.baseMaxHealth
        if (health in zealotHealth || health.derpy() in zealotHealth) return true
        return entity.getLorenzVec().getEntitiesNearby<ArmorStand>(NAMETAG_RADIUS)
            .any { it.name.string.contains("Zealot") }
    }

    /** Switching to another server starts the count over. */
    @HandleEvent
    private fun onWorldChange(event: WorldChangeEvent) {
        hitZealots.clear()
        bestiaryKills = null
        kills = 0
    }
}
