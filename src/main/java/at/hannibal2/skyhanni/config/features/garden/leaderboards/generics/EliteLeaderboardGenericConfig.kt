package at.hannibal2.skyhanni.config.features.garden.leaderboards.generics

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

open class EliteLeaderboardGenericConfig<RankGoal : RankGoalGenericConfig, Display : EliteDisplayGenericConfig>(
    rankGoalConfig: () -> RankGoal,
    displayConfig: () -> Display
) {
    @Expose
    @ConfigOption(name = "Display", desc = "")
    @Accordion
    val display: Display = displayConfig()

    @Expose
    @ConfigOption(name = "Rank Goals", desc = "")
    @Accordion
    val rankGoals: RankGoal = rankGoalConfig()

    @Expose
    @ConfigOption(
        name = "Show LB Change",
        desc = "Show the change of your position on your current pest leaderboard while you were offline."
    )
    @ConfigEditorBoolean
    var showLbChange: Boolean = false
}
