package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.fishing.FishingBobberInLiquidEvent
import at.hannibal2.skyhanni.events.fishing.FishingCatchEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text

@SkyHanniModule
object FishingBobberTimer {

    private val config get() = SkyHanniMod.feature.fishing.bobberTimer

    private var deployTime: SimpleTimeMark = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnSkyblock = true)
    private fun onBobberCast() {
        if (config.startOnLiquidTouch) {
            deployTime = SimpleTimeMark.farPast()
        } else {
            deployTime = SimpleTimeMark.now()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onBobberInLiquid(event: FishingBobberInLiquidEvent) {
        if (config.startOnLiquidTouch) deployTime = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onFishingCatch(event: FishingCatchEvent) {
        deployTime = SimpleTimeMark.farPast()
    }

    @HandleEvent
    private fun onWorldChange() {
        deployTime = SimpleTimeMark.farPast()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onGuiRenderOverlay() {
        if (!config.enabled) return
        if (deployTime.isFarPast()) return
        if (!FishingApi.holdingRod) return
        if (FishingApi.bobber == null) {
            deployTime = SimpleTimeMark.farPast()
            return
        }
        val elapsed = deployTime.passedSince()
        val display = Renderable.text("§aBobber: §f${elapsed.format(showMilliSeconds = true)}")
        config.pos.renderRenderable(display, posLabel = "Fishing Bobber Timer")
    }
}
