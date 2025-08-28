package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.config.features.garden.leaderboards.PestKillsDisplayConfig.PestTypeWithAll
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.features.garden.GardenApi
import io.github.notenoughupdates.moulconfig.observer.Property
import kotlin.reflect.KProperty0

object EliteFarmersRankGoals {
    private val weightConfig get() = GardenApi.config.eliteFarmersLeaderboards.farmingWeightDisplay
    private val cropConfig get() = GardenApi.config.eliteFarmersLeaderboards.cropCollectionDisplay
    private val pestConfig get() = GardenApi.config.eliteFarmersLeaderboards.pestKillsDisplay

    fun getRankFromConfig(leaderboardType: EliteLeaderboardType) = when (leaderboardType) {
        is EliteLeaderboardType.Weight -> if (weightConfig.useRankGoal.get()) {
            getLeaderboardRankConfig(leaderboardType)?.get()
        } else null
        is EliteLeaderboardType.Crop -> if (cropConfig.useRankGoal.get() && leaderboardType.crop in cropConfig.rankGoalCrops.get()) {
            getLeaderboardRankConfig(leaderboardType)?.get()
        } else null
        is EliteLeaderboardType.Pest -> {
            // config has an "All pests" option so we want to cast to that and assume a null option is all pests
            val pestWithAll = leaderboardType.pest?.let { PestTypeWithAll.Specific(it) } ?: PestTypeWithAll.AllPests

            if (pestConfig.useRankGoal.get() && pestWithAll in pestConfig.rankGoalPests.get()) {
                getLeaderboardRankConfig(leaderboardType)?.get()
            } else null
        }
    }


    fun getLeaderboardRankConfig(type: EliteLeaderboardType): KProperty0<Property<String>>? = when (type) {
        is EliteLeaderboardType.Weight -> when (type.mode) {
            EliteLeaderboardMode.ALL_TIME -> weightConfig::weightRankGoal
            EliteLeaderboardMode.MONTHLY -> weightConfig::monthlyWeightRankGoal
        }

        is EliteLeaderboardType.Crop -> when (type.mode) {
            EliteLeaderboardMode.ALL_TIME -> cropConfig.cropRankGoalsConfig.get().goalMap[type.crop]
            EliteLeaderboardMode.MONTHLY -> cropConfig.monthlyCropRankGoalsConfig.get().goalMap[type.crop]
        }

        is EliteLeaderboardType.Pest -> when (type.mode) {
            EliteLeaderboardMode.ALL_TIME -> pestConfig.pestRankGoalsConfig.get().goalMap[type.pest]
                ?: pestConfig.pestRankGoalsConfig.get().goalMap[null]

            EliteLeaderboardMode.MONTHLY -> pestConfig.monthlyPestRankGoalsConfig.get().goalMap[type.pest]
                ?: pestConfig.monthlyPestRankGoalsConfig.get().goalMap[null]
        }
    }
}
