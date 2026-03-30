package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
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

    val display = mutableListOf<Renderable>()

    init {
        Deployable.entries.forEach { it.pattern }
    }

    private enum class Deployable(
        deployableName: String,
        val displayName: String,
        val range: Int,
        val fullShaft: Boolean = false,
        var entity: LivingEntity? = null,
        var expiryTime: SimpleTimeMark = SimpleTimeMark.farPast(),
    ) {
        PLASMAFLUX("Plasmaflux", "§l§dPlasmaflux", 20),
        OVERFLUX("Overflux", "§5Overflux", 18),
        MANA_FLUX("Mana Flux", "§9Mana Flux", 18),
        RADIANT("Radiant", "§aRadiant", 18),
        DWARVEN_LANTERN("Dwarven Lantern", "§fDwarven Lantern", 30),
        MITHRIL_LANTERN("Mithril Lantern", "§aMithril Lantern", 30),
        TITANIUM_LANTERN("Titanium Lantern", "§9Titanium Lantern", 30),
        GLACITE_LANTERN("Glacite Lantern", "§5Glacite Lantern", 30, true),
        WILL_O_WISP("Will-o'-wisp", "§6Will-o'-wisp", 30, true),
        BLACK_HOLE("Black Hole", "§5Black Hole", 10),
        UMBERELLA("Umberella", "§9Umberella", 30);


        val pattern by group.pattern(
            name.lowercase().replace("_", "-"),
            "$deployableName (?<time>\\d+)s",
        )

        fun isInRange(entity: LivingEntity): Boolean {
            val shaftBuff = fullShaft && IslandType.MINESHAFT.isCurrent()
            return shaftBuff || range > entity.getLorenzVec().distanceToPlayer()
        }

        fun isInRange(): Boolean {
            val shaftBuff = fullShaft && IslandType.MINESHAFT.isCurrent()
            val entity = entity ?: return false
            return shaftBuff || range > entity.getLorenzVec().distanceToPlayer()
        }

        fun isActive(): Boolean {
            return !expiryTime.isInPast() && isInRange() && entity?.deceased == false
        }
    }

    private val activeDeployables = mutableListOf<Deployable>()

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntitySpawn(event: EntityCustomNameUpdateEvent<ArmorStand>) {
        val entity = event.entity
        for (deployable in Deployable.entries) {
            deployable.pattern.matchMatcher(entity.name) {
                if (!deployable.isInRange(entity)) return@matchMatcher
                val time = SimpleTimeMark.now() + group("time").formatInt().toDuration(DurationUnit.SECONDS)
                if (deployable.expiryTime > time && deployable.isActive()) return@matchMatcher
                deployable.entity = entity
                deployable.expiryTime = time
                for (entry in activeDeployables) {
                    val entryEntity = entry.entity ?: break
                    if (entryEntity.getLorenzVec().equalsIgnoreY(entity.getLorenzVec())) return@matchMatcher
                }
                activeDeployables.add(deployable)
            }
        }
    }

    val position: Position = Position(150, 200)

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (display.isEmpty()) return
        position.renderRenderables(display, 0, "orb")
    }
    
    @HandleEvent(onlyOnSkyblock = true)
    fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(10)) return
        buildDisplay()
    }

    fun buildDisplay() {
        activeDeployables.removeIf { !it.isActive() }
        display.clear()
        for (deployable in activeDeployables) {
            display.add(Renderable.text("${deployable.displayName} §e${deployable.expiryTime.timeUntil().format()}"))
        }
    }
}