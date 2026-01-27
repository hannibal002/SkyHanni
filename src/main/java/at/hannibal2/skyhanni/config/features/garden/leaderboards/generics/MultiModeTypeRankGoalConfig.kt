package at.hannibal2.skyhanni.config.features.garden.leaderboards.generics

import at.hannibal2.skyhanni.config.features.garden.leaderboards.PestTypeWithAll.Companion.fromPestType
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import kotlin.reflect.KProperty0

abstract class MultiModeTypeRankGoalConfig<
    E : Enum<E>,
    Config : TypeRankGoalGenericConfig<E>,
    ConfigMonthly : TypeRankGoalGenericConfig<E>,
    >(
    createConfig: () -> Config,
    createMonthlyConfig: () -> ConfigMonthly,
) : RankGoalGenericConfig() {
    // moulconfig requires concrete types
    @Suppress("StorageNeedsExpose")
    abstract val rankGoalTypes: Property<MutableList<E>>

    @Expose
    @ConfigOption(
        name = "All-Time Rank Goals",
        desc = "",
    )
    @Accordion
    val rankGoalsConfig: Config = createConfig()

    @Expose
    @ConfigOption(
        name = "Monthly Rank Goals",
        desc = "",
    )
    @Accordion
    val monthlyRankGoalsConfig: ConfigMonthly = createMonthlyConfig()

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
