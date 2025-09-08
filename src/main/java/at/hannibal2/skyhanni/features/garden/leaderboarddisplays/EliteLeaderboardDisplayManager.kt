package at.hannibal2.skyhanni.features.garden.leaderboarddisplays

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.leaderboards.EliteLeaderboardConfigApi.getLeaderboardRankConfig
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.clearCategories
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.clearEntries
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
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
        cropDisplay.renderDisplay(cropConfig.display.pos)
        pestDisplay.renderDisplay(pestConfig.display.pos)
        weightDisplay.renderDisplay(weightConfig.display.pos)
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

    private val weightConfigs = listOf(
        weightConfig.rankGoals.useRankGoal,
        weightConfig.rankGoals.monthlyRankGoal,
        weightConfig.rankGoals.rankGoal
    )

    private val cropConfigs = listOf(
        cropConfig.rankGoals.useRankGoal,
        cropConfig.rankGoals.rankGoalTypes,
    )

    private val pestConfigs = listOf(
        pestConfig.rankGoals.useRankGoal,
        pestConfig.rankGoals.rankGoalTypes,
    )

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        weightConfigs.forEach {
            it.afterChange {
                clearCategories(EliteLeaderboardType.Weight::class)
            }
        }

        cropConfigs.forEach {
            it.afterChange {
                ChatUtils.debug("Change: $it")
                clearCategories(EliteLeaderboardType.Crop::class)
            }
        }

        pestConfigs.forEach {
            it.afterChange {
                clearCategories(EliteLeaderboardType.Pest::class)
            }
        }

        for (crop in CropType.entries) {
            for (mode in EliteLeaderboardMode.entries) {
                val leaderboardType = EliteLeaderboardType.Crop(crop, mode)
                ConditionalUtils.onToggle(getLeaderboardRankConfig(leaderboardType)?.get() ?: continue) {
                    clearEntries(leaderboardType)
                }
            }
        }

        for (pest in (PestType.entries + null)) {
            for (mode in EliteLeaderboardMode.entries) {
                val leaderboardType = EliteLeaderboardType.Pest(pest, mode)
                ConditionalUtils.onToggle(getLeaderboardRankConfig(leaderboardType)?.get() ?: continue) {
                    clearEntries(leaderboardType)
                }
            }
        }
    }

    // TODO a shit ton of config fixes
}
