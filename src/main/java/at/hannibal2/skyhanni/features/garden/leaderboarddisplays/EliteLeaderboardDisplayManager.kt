package at.hannibal2.skyhanni.features.garden.leaderboarddisplays

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.garden.elitefarmers.LeaderboardData.clearCategories
import at.hannibal2.skyhanni.data.garden.elitefarmers.LeaderboardData.clearEntries
import at.hannibal2.skyhanni.data.garden.elitefarmers.LeaderboardConfigApi.getRankFromConfig
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange

@SkyHanniModule
object EliteLeaderboardDisplayManager {
    val config get() = GardenApi.config.eliteFarmersLeaderboards
    private val cropConfig get() = config.cropCollectionDisplay
    private val pestConfig get() = config.pestKillsDisplay
    private val weightConfig get() = config.farmingWeightDisplay

    private val pestDisplay = PestDisplay()
    private val cropDisplay = CropDisplay()
    private val weightDisplay = WeightDisplay()

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

    fun updateDisplays() {
        cropDisplay.update()
        pestDisplay.update()
        weightDisplay.update()
    }

    fun resetDisplays() {
        cropDisplay.reset()
        pestDisplay.reset()
        weightDisplay.reset()
    }
}
