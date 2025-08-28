package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.config.features.garden.leaderboards.PestKillsDisplayConfig.PestTypeWithAll
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.utils.ChatUtils
import io.github.notenoughupdates.moulconfig.observer.Property
import kotlin.reflect.KProperty0

object EliteFarmersRankGoals {
    private val weightConfig get() = GardenApi.config.eliteFarmersLeaderboards.farmingWeightDisplay
    private val cropConfig get() = GardenApi.config.eliteFarmersLeaderboards.cropCollectionDisplay
    private val pestConfig get() = GardenApi.config.eliteFarmersLeaderboards.pestKillsDisplay

    fun getRankFromConfig(leaderboardType: EliteLeaderboardType): Property<String>? {
        return when (leaderboardType) {
            is EliteLeaderboardType.Weight -> if (weightConfig.useRankGoal.get()) {
                getLeaderboardRankConfig(leaderboardType).get()
            } else null
            is EliteLeaderboardType.Crop -> if (cropConfig.useRankGoal.get() && leaderboardType.crop in cropConfig.rankGoalCrops.get()) {
                getLeaderboardRankConfig(leaderboardType).get()
            } else null
            is EliteLeaderboardType.Pest -> {
                if (pestConfig.useRankGoal.get() && PestTypeWithAll.fromPestType(leaderboardType.pest) in pestConfig.rankGoalPests.get()) {
                    getLeaderboardRankConfig(leaderboardType).get()
                } else null
            }
        }
    }

    fun getLeaderboardRankConfig(type: EliteLeaderboardType): KProperty0<Property<String>> = when (type) {
        is EliteLeaderboardType.Weight -> when (type.mode) {
            EliteLeaderboardMode.ALL_TIME -> weightConfig::weightRankGoal
            EliteLeaderboardMode.MONTHLY -> weightConfig::monthlyWeightRankGoal
        }

        is EliteLeaderboardType.Crop -> when (type.mode) {
            EliteLeaderboardMode.ALL_TIME -> cropConfig.cropRankGoalsConfig.getGoal(type.crop)
            EliteLeaderboardMode.MONTHLY -> cropConfig.monthlyCropRankGoalsConfig.getGoal(type.crop)
        }

        is EliteLeaderboardType.Pest -> when (type.mode) {
            EliteLeaderboardMode.ALL_TIME -> pestConfig.pestRankGoalsConfig.getGoal(type.pest)
            EliteLeaderboardMode.MONTHLY -> pestConfig.monthlyPestRankGoalsConfig.getGoal(type.pest)
        }
    }
}
