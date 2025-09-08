package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.EliteLeaderboardGenericConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.rankgoals.MultipleTypeRankGoalConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.rankgoals.RankGoalGenericConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.rankgoals.SingleTypeRankGoalConfig
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.features.garden.GardenApi
import io.github.notenoughupdates.moulconfig.observer.Property
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0

object EliteFarmersRankGoals {
    private val weightConfig get() = GardenApi.config.eliteFarmersLeaderboards.farmingWeightDisplay
    private val cropConfig get() = GardenApi.config.eliteFarmersLeaderboards.cropCollectionDisplay
    private val pestConfig get() = GardenApi.config.eliteFarmersLeaderboards.pestKillsDisplay

    fun getConfig(leaderboardType: EliteLeaderboardType): EliteLeaderboardGenericConfig<*, *> = when (leaderboardType) {
        is EliteLeaderboardType.Weight -> weightConfig
        is EliteLeaderboardType.Crop -> cropConfig
        is EliteLeaderboardType.Pest -> pestConfig
    }

    fun getConfigFromClass(leaderboardType: KClass<out EliteLeaderboardType>): EliteLeaderboardGenericConfig<*, *>? = when (leaderboardType) {
        EliteLeaderboardType.Weight::class -> weightConfig
        EliteLeaderboardType.Crop::class -> cropConfig
        EliteLeaderboardType.Pest::class -> pestConfig
        else -> null
    }

    fun getRankFromConfig(leaderboardType: EliteLeaderboardType): Property<String>? =
        getLeaderboardRankConfig(leaderboardType)?.get()

    fun getLeaderboardRankConfig(leaderboardType: EliteLeaderboardType): KProperty0<Property<String>>? =
        when (val config = getRankConfig(leaderboardType)) {
        is SingleTypeRankGoalConfig -> when (leaderboardType.mode) {
            EliteLeaderboardMode.ALL_TIME -> config::rankGoal
            EliteLeaderboardMode.MONTHLY -> config::monthlyRankGoal
        }
        is MultipleTypeRankGoalConfig<*, *> -> config.getGoal(leaderboardType)
        else -> null
    }


    fun getRankConfig(leaderboardType: EliteLeaderboardType): RankGoalGenericConfig = getConfig(leaderboardType).rankGoals

    fun getRankConfig(leaderboardType: KClass<out EliteLeaderboardType>?): RankGoalGenericConfig? = when (leaderboardType) {
        EliteLeaderboardType.Weight::class -> weightConfig.rankGoals
        EliteLeaderboardType.Crop::class -> cropConfig.rankGoals
        EliteLeaderboardType.Pest::class -> pestConfig.rankGoals
        else -> null
    }
}
