package at.hannibal2.skyhanni.features.combat.cocoon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.combat.CocoonSpawnEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CocoonOverlay {
    private val config get() = SkyHanniMod.feature.combat.cocoonOverlay

    private val currentCocoons: TimeLimitedSet<CocoonAPI.CocoonMob> = TimeLimitedSet(8.seconds)

    private val LIFETIME = 6.4.seconds

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        for (cocoon in currentCocoons) {
            val timeTillSpawn = (LIFETIME - cocoon.spawnTime.passedSince()).format(showMilliSeconds = true)
            if (config.showCocoonContainedMobName) event.drawDynamicText(cocoon.coordinates, cocoon.mob.name, 2.0, yOff = 0f, seeThroughBlocks = false)
            if (config.showCocoonTimerTillHatch) event.drawDynamicText(cocoon.coordinates, timeTillSpawn, 2.0, yOff = -20f, seeThroughBlocks = false)
        }
    }

    @HandleEvent
    fun onCocoon(event: CocoonSpawnEvent) {
        currentCocoons.add(event.cocoonMob)
    }
}
