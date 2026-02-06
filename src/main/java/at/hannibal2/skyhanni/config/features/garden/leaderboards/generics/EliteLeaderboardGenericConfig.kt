package at.hannibal2.skyhanni.config.features.garden.leaderboards.generics

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

open class EliteLeaderboardGenericConfig<RankGoal : RankGoalGenericConfig, Display : EliteDisplayGenericConfig>(
    rankGoalConfig: () -> RankGoal,
    displayConfig: () -> Display,
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
        name = "Player Overtake Messages",
        desc = "Send a message in chat when you overtake a player on the leaderboard.",
    )
    @ConfigEditorBoolean
    var showLbChange: Boolean = true

    @Expose
    @ConfigOption(
        name = "Show Offline LB Change",
        desc = "Show the change of your position on your current leaderboard while you were offline.",
    )
    @ConfigEditorBoolean
    var offlineLbChange: Boolean = false

    @Expose
    @ConfigOption(
        name = "Gamemode",
        desc = "Which game mode to show on the leaderboard.",
    )
    @ConfigEditorDropdown
    val gamemode: Property<Gamemode> = Property.of(Gamemode.ALL)
}

enum class Gamemode(val apiMode: String?, val displayName: String, val renderableName: String) {
    ALL(null, "All", ""),
    IRONMAN("ironman", "Ironman", " Ironman"),
    STRANDED("island", "Stranded", " Stranded")
    ;

    override fun toString(): String {
        return this.displayName
    }
}
