package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.compat.deceased
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@SkyHanniModule
object DeployableDisplay {

    private val group = RepoPattern.group("deployable")
    private val config get() = SkyHanniMod.feature.combat.deployable

    private val display = mutableListOf<Renderable>()

    init {
        Deployable.entries.forEach { it.pattern }
    }

    private enum class Deployable(
        deployableName: String,
        val displayName: String,
        val range: Int,
        val type: DeployableType,
        val tier: Int = 0,
        val fullShaft: Boolean = false,
        var entity: LivingEntity? = null,
        var expiryTime: SimpleTimeMark = SimpleTimeMark.farPast(),
    ) {
        RADIANT("Radiant", "§aRadiant", 18, DeployableType.FLUX, 1),
        MANA_FLUX("Mana Flux", "§9Mana Flux", 18, DeployableType.FLUX, 2),
        OVERFLUX("Overflux", "§5Overflux", 18, DeployableType.FLUX, 3),
        PLASMAFLUX("Plasmaflux", "§d§lPlasmaflux", 20, DeployableType.FLUX, 4),
        DWARVEN_LANTERN("Dwarven Lantern", "§fDwarven Lantern", 30, DeployableType.LANTERN, 1),
        MITHRIL_LANTERN("Mithril Lantern", "§aMithril Lantern", 30, DeployableType.LANTERN, 2),
        TITANIUM_LANTERN("Titanium Lantern", "§9Titanium Lantern", 30, DeployableType.LANTERN, 3),
        GLACITE_LANTERN("Glacite Lantern", "§5Glacite Lantern", 30, DeployableType.LANTERN, 4, true),
        WILL_O_WISP("Will-o'-wisp", "§6§lWill-o'-wisp", 30, DeployableType.LANTERN, 5, true),
        BLACK_HOLE("Black Hole", "§5Black Hole", 20, DeployableType.BLACK_HOLE),
        UMBERELLA("Umberella", "§9Umberella", 30, DeployableType.UMBERELLA),
        ;

        val pattern by group.pattern(
            name.lowercase().replace("_", "-"),
            "$deployableName (?<time>\\d+)s",
        )

        fun isInRange(entity: LivingEntity): Boolean {
            return hasShaftBuff() || range > entity.getLorenzVec().distanceToPlayer()
        }

        fun isInRange(): Boolean {
            if (hasShaftBuff()) return true
            val entity = entity ?: return false
            return range > entity.getLorenzVec().distanceToPlayer()
        }

        fun hasShaftBuff(): Boolean {
            return fullShaft && IslandType.MINESHAFT.isCurrent()
        }

        fun isActive(): Boolean {
            // A mineshaft is bigger than entity render distance
            return !expiryTime.isInPast() && isInRange() && (entity?.deceased == false || hasShaftBuff())
        }

        fun reset() {
            entity = null
            expiryTime = SimpleTimeMark.farPast()
        }

        override fun toString(): String {
            return displayName
        }
    }

    enum class DeployableType {
        FLUX,
        LANTERN,
        UMBERELLA,
        BLACK_HOLE,
        ;

        override fun toString(): String {
            return when {
                this == FLUX -> "Power Orb §7(§d§lPlasmaflux§7)"
                this == LANTERN -> "Lantern §7(§5Glacite Lantern§7)"
                this == UMBERELLA -> "§9Umberella"
                this == BLACK_HOLE -> "§5Black Hole"
                else -> "error sob emoji"
            }
        }
    }

    // todo should this be a set?
    private val activeDeployables = mutableListOf<Deployable>()

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntitySpawn(event: EntityCustomNameUpdateEvent<ArmorStand>) {
        if (!config.enabled) return
        val entity = event.entity
        for (deployable in Deployable.entries) {
            deployable.pattern.matchMatcher(entity.name) {
                if (!deployable.isInRange(entity)) return@matchMatcher
                val time = SimpleTimeMark.now() + group("time").formatInt().toDuration(DurationUnit.SECONDS)
                if (deployable.expiryTime > time && deployable.isActive()) return@matchMatcher
                deployable.entity = entity
                deployable.expiryTime = time
                for (entry in activeDeployables) {
                    val entryEntity = entry.entity ?: continue
                    if (entryEntity.getLorenzVec().equalsIgnoreY(entity.getLorenzVec())) return@matchMatcher
                }
                activeDeployables.add(deployable)
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay() {
        if (!config.enabled) return
        if (display.isEmpty()) return
        config.position.renderRenderables(display, 0, "Deployable Overlay")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick(event: SkyHanniTickEvent) {
        if (!config.enabled) return
        if (!event.isMod(10)) return
        buildDisplay()
    }

    @HandleEvent
    fun onWorldChange() {
        activeDeployables.clear()
        Deployable.entries.forEach { it.reset() }
    }

    private fun buildDisplay() {
        activeDeployables.removeIf { !it.isActive() }
        Deployable.entries.forEach { if (!it.isActive()) it.reset() }
        display.clear()
        for (deployable in activeDeployables) {
            if (deployable.type !in config.displayTypes) continue
            if (config.highestTierOnly && activeDeployables.any { it.type == deployable.type && it.tier > deployable.tier }) continue
            display.add(Renderable.text("$deployable §e${deployable.expiryTime.timeUntil().format()}"))
        }
    }
}
