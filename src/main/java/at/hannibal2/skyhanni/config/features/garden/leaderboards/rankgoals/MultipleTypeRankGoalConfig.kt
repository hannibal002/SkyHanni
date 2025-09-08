package at.hannibal2.skyhanni.config.features.garden.leaderboards.rankgoals

import at.hannibal2.skyhanni.config.features.garden.leaderboards.PestTypeWithAll.Companion.fromPestType
import at.hannibal2.skyhanni.config.features.garden.leaderboards.rankgoals.types.TypeRankGoalGenericConfig
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import kotlin.reflect.KProperty0

abstract class MultipleTypeRankGoalConfig<E: Enum<E>, Config: TypeRankGoalGenericConfig<E>>(
    createConfig: () -> Config
): RankGoalGenericConfig() {
    // moulconfig requires concrete types
    abstract val rankGoalTypes: Property<MutableList<E>>

    @Expose
    @ConfigOption(
        name = "All-Time Rank Goals",
        desc = ""
    )
    @Accordion
    val rankGoalsConfig: Config = createConfig()

    @Expose
    @ConfigOption(
        name = "Monthly Rank Goals",
        desc = ""
    )
    @Accordion
    val monthlyRankGoalsConfig: Config = createConfig()

    @Suppress("UNCHECKED_CAST")
    fun getGoal(leaderboardType: EliteLeaderboardType): KProperty0<Property<String>> {
        val type = if (leaderboardType is EliteLeaderboardType.Pest) {
            fromPestType(leaderboardType.pest) as? E
        } else {
            leaderboardType.type as? E
        } ?: throw IllegalArgumentException("LeaderboardType $leaderboardType is not supported")

        return when (leaderboardType.mode) {
            EliteLeaderboardMode.ALL_TIME -> rankGoalsConfig.getConfig(type)
            EliteLeaderboardMode.MONTHLY -> monthlyRankGoalsConfig.getConfig(type)
        }
    }
}
