package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object EliteLeaderboardDisplays {
    val config get() = GardenApi.config.eliteFarmersLeaderboards
    val cropConfig get() = config.cropCollectionDisplay

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        cropDisplay.update()
    }

    @HandleEvent
    fun onRender(event: GuiRenderEvent) {
        cropDisplay.renderDisplay(cropConfig.pos)
    }

    val cropDisplay = CropDisplay()

    // TODO a shit ton of config fixes
}
