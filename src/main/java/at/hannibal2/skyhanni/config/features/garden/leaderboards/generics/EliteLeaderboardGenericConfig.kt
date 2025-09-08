package at.hannibal2.skyhanni.config.features.garden.leaderboards.generics

import at.hannibal2.skyhanni.config.features.garden.leaderboards.rankgoals.RankGoalGenericConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

open class EliteLeaderboardGenericConfig<E: RankGoalGenericConfig, T: EliteDisplayGenericConfig>(
    rankGoalConfig: () -> E,
    displayConfig: () -> T
) {
    @Expose
    @ConfigOption(name = "Display", desc = "")
    @Accordion
    val display: T = displayConfig()

    @Expose
    @ConfigOption(name = "Rank Goals", desc = "")
    @Accordion
    val rankGoals: E = rankGoalConfig()

    @Expose
    @ConfigOption(
        name = "Show LB Change",
        desc = "Show the change of your position on your current pest leaderboard while you were offline."
    )
    @ConfigEditorBoolean
    var showLbChange: Boolean = false
}
