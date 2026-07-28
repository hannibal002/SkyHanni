package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.world.entity.decoration.ArmorStand
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@SkyHanniModule
object DeployableDisplay {

    private val config get() = SkyHanniMod.feature.combat.deployable

    private val display = mutableListOf<Renderable>()

    init {
        Deployable.entries.forEach { it.pattern }
    }

    // todo should this be a set?
    private val activeDeployables = mutableListOf<Deployable>()

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntitySpawn(event: EntityCustomNameUpdateEvent<ArmorStand>) {
        // TODO: Have some way to not list all the features that need this
        if (!config.enabled && !config.deployableReminder.enabled) return
        val entity = event.entity
        for (deployable in Deployable.entries) {
            deployable.pattern.matchMatcher(entity.cleanName) {
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

    fun getActiveDeployables(): List<Deployable> {
        return activeDeployables.toList()
    }
}
