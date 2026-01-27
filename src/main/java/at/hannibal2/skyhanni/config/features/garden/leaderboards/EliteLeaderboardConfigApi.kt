package at.hannibal2.skyhanni.config.features.garden.leaderboards

import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.EliteDisplayGenericConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.EliteLeaderboardGenericConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.MultiModeTypeRankGoalConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.RankGoalGenericConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.SingleTypeRankGoalConfig
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.features.garden.GardenApi
import io.github.notenoughupdates.moulconfig.observer.Property
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0

object EliteLeaderboardConfigApi {

    private val weightConfig get() = GardenApi.config.eliteFarmersLeaderboards.farmingWeightLeaderboard
    private val cropConfig get() = GardenApi.config.eliteFarmersLeaderboards.cropCollectionLeaderboard
    private val pestConfig get() = GardenApi.config.eliteFarmersLeaderboards.pestKillsLeaderboard

    fun getLeaderboardConfig(leaderboardType: EliteLeaderboardType): EliteLeaderboardGenericConfig<*, *> = when (leaderboardType) {
        is EliteLeaderboardType.Weight -> weightConfig
        is EliteLeaderboardType.Crop -> cropConfig
        is EliteLeaderboardType.Pest -> pestConfig
    }

    fun getConfigFromClass(leaderboardType: KClass<out EliteLeaderboardType>): EliteLeaderboardGenericConfig<*, *>? =
        when (leaderboardType) {
            EliteLeaderboardType.Weight::class -> weightConfig
            EliteLeaderboardType.Crop::class -> cropConfig
            EliteLeaderboardType.Pest::class -> pestConfig
            else -> null
        }

    fun getRankGoalIfValid(leaderboardType: EliteLeaderboardType): Property<String>? {
        val config = getRankConfig(leaderboardType)
        if (!config.useRankGoal.get()) return null
        if (config is MultiModeTypeRankGoalConfig<*, *, *> && leaderboardType.type !in config.rankGoalTypes.get()) return null
        return getLeaderboardRankConfig(leaderboardType)?.get()
    }

    fun getLeaderboardRankConfig(leaderboardType: EliteLeaderboardType): KProperty0<Property<String>>? =
        when (val config = getRankConfig(leaderboardType)) {
            is SingleTypeRankGoalConfig -> when (leaderboardType.mode) {
                EliteLeaderboardMode.ALL_TIME -> config::rankGoal
                EliteLeaderboardMode.MONTHLY -> config::monthlyRankGoal
            }
            is MultiModeTypeRankGoalConfig<*, *, *> -> config.getGoal(leaderboardType)
            else -> null
        }

    fun getRankConfig(leaderboardType: EliteLeaderboardType): RankGoalGenericConfig = getLeaderboardConfig(leaderboardType).rankGoals

    fun getDisplayConfig(leaderboardType: EliteLeaderboardType): EliteDisplayGenericConfig = getLeaderboardConfig(leaderboardType).display
}
