package at.hannibal2.skyhanni.features.garden.leaderboarddisplays

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.clearCategories
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.clearEntries
import at.hannibal2.skyhanni.data.garden.EliteFarmersRankGoals.getRankFromConfig
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


    fun getLeaderboardChangeConfig(leaderboardType: EliteLeaderboardType) = when (leaderboardType) {
        is EliteLeaderboardType.Pest -> pestConfig.showLbChange
        is EliteLeaderboardType.Weight -> weightConfig.showLbChange
        is EliteLeaderboardType.Crop -> cropConfig.showLbChange
    }

    fun getRankGoalConfig(leaderboardType: EliteLeaderboardType) = when (leaderboardType) {
        is EliteLeaderboardType.Pest -> pestConfig.useRankGoal
        is EliteLeaderboardType.Weight -> weightConfig.useRankGoal
        is EliteLeaderboardType.Crop -> cropConfig.useRankGoal
    }

    private val weightConfigs = listOf(
        weightConfig.useRankGoal,
        weightConfig.monthlyWeightRankGoal,
        weightConfig.weightRankGoal
    )

    private val cropConfigs = listOf(
        cropConfig.useRankGoal,
        cropConfig.rankGoalCrops,
    )

    private val pestConfigs = listOf(
        pestConfig.useRankGoal,
        pestConfig.rankGoalPests,
    )

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        weightConfigs.forEach { it.afterChange {
            clearCategories(EliteLeaderboardType.Weight::class)
        } }

        cropConfigs.forEach { it.afterChange {
            clearCategories(EliteLeaderboardType.Crop::class)
        } }

        pestConfigs.forEach { it.afterChange {
            clearCategories(EliteLeaderboardType.Pest::class)
        } }

        for (crop in CropType.entries) {
            for (mode in EliteLeaderboardMode.entries) {
                val leaderboardType = EliteLeaderboardType.Crop(crop, mode)
                ConditionalUtils.onToggle(getRankFromConfig(leaderboardType) ?: continue) {
                    clearEntries(leaderboardType)
                }
            }
        }

        for (pest in (PestType.entries + null)) {
            for (mode in EliteLeaderboardMode.entries) {
                val leaderboardType = EliteLeaderboardType.Pest(pest, mode)
                ConditionalUtils.onToggle(getRankFromConfig(leaderboardType) ?: continue) {
                    clearEntries(leaderboardType)
                }
            }
        }
    }

    // TODO a shit ton of config fixes
}
