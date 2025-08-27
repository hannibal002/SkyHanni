package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.garden.pests.PestKillEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object EliteLeaderboardDisplays {
    val config get() = GardenApi.config.eliteFarmersLeaderboards
    val cropConfig get() = config.cropCollectionDisplay
    val pestConfig get() = config.pestKillsDisplay
    val weightConfig get() = config.farmingWeightDisplay

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        cropDisplay.update()
        pestDisplay.update()
        weightDisplay.update()
    }

    @HandleEvent
    fun onRender(event: GuiRenderEvent) {
        cropDisplay.renderDisplay(cropConfig.pos)
        pestDisplay.renderDisplay(pestConfig.pos)
        weightDisplay.renderDisplay(weightConfig.pos)
    }

    val pestDisplay = PestDisplay()
    val cropDisplay = CropDisplay()
    val weightDisplay = WeightDisplay()

    // TODO a shit ton of config fixes
}
