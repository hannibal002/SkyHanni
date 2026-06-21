package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.fishing.FishingBobberCastEvent
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
    fun onBobberCast(event: FishingBobberCastEvent) {
        if (config.startOnLiquidTouch) {
            deployTime = SimpleTimeMark.farPast()
        } else {
            deployTime = SimpleTimeMark.now()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onBobberInLiquid(event: FishingBobberInLiquidEvent) {
        if (config.startOnLiquidTouch) deployTime = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onFishingCatch(event: FishingCatchEvent) {
        deployTime = SimpleTimeMark.farPast()
    }

    @HandleEvent
    fun onWorldChange() {
        deployTime = SimpleTimeMark.farPast()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled) return
        if (deployTime.isFarPast()) return
        if (FishingApi.bobber == null) {
            deployTime = SimpleTimeMark.farPast()
            return
        }
        val elapsed = deployTime.passedSince()
        val display = Renderable.text("§aBobber: §f${elapsed.format(showMilliSeconds = true)}")
        config.pos.renderRenderable(display, posLabel = "Fishing Bobber Timer")
    }
}
