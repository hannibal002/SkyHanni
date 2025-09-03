package at.hannibal2.skyhanni.data.garden.elitefarmers

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.leaderboards.PestKillsDisplayConfigTEST.PestTypeWithAll
import at.hannibal2.skyhanni.data.garden.elitefarmers.LeaderboardData.clearCategories
import at.hannibal2.skyhanni.data.garden.elitefarmers.LeaderboardData.clearEntries
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import io.github.notenoughupdates.moulconfig.observer.Property
import kotlin.reflect.KProperty0

@SkyHanniModule
object LeaderboardConfigApi {
    private val weightConfig get() = GardenApi.config.eliteFarmersLeaderboards.farmingWeightDisplay
    private val cropConfig get() = GardenApi.config.eliteFarmersLeaderboards.cropCollectionDisplay
    private val pestConfig get() = GardenApi.config.eliteFarmersLeaderboards.pestKillsDisplay

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
        weightConfigs.forEach {
            it.afterChange {
                clearCategories(EliteLeaderboardType.Weight::class)
            }
        }

        cropConfigs.forEach {
            it.afterChange {
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
    fun getOvertakeMessageConfig(leaderboardType: EliteLeaderboardType) = when (leaderboardType) {
        is EliteLeaderboardType.Pest -> pestConfig.offlineChangeMessage
        is EliteLeaderboardType.Weight -> weightConfig.offlineChangeMessage
        is EliteLeaderboardType.Crop -> cropConfig.offlineChangeMessage
    }


    fun getLeaderboardChangeConfig(leaderboardType: EliteLeaderboardType) = when (leaderboardType) {
        is EliteLeaderboardType.Pest -> pestConfig.offlineChangeMessage
        is EliteLeaderboardType.Weight -> weightConfig.offlineChangeMessage
        is EliteLeaderboardType.Crop -> cropConfig.offlineChangeMessage
    }

    fun getRankGoalConfig(leaderboardType: EliteLeaderboardType) = when (leaderboardType) {
        is EliteLeaderboardType.Pest -> pestConfig.useRankGoal
        is EliteLeaderboardType.Weight -> weightConfig.useRankGoal
        is EliteLeaderboardType.Crop -> cropConfig.useRankGoal
    }

    fun getLBChange(leaderboardType: EliteLeaderboardType) = getConfig(leaderboardType).offlineChangeMessage

    fun getConfig(leaderboardType: EliteLeaderboardType) = when (leaderboardType) {
        is EliteLeaderboardType.Pest -> pestConfig
        is EliteLeaderboardType.Weight -> weightConfig
        is EliteLeaderboardType.Crop -> cropConfig
    }

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
