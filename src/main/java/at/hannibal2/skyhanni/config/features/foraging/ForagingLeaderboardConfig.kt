package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.EliteDisplayGenericConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.EliteLeaderboardGenericConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.MultiTypeRankGoalConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.rankgoals.ForagingLogTypeRankGoalsConfig
import at.hannibal2.skyhanni.features.foraging.ForagingLogType
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class ForagingLeaderboardConfig : EliteLeaderboardGenericConfig<
    ForagingLogRankGoalConfig,
    ForagingLeaderboardDisplayConfig
    >(
    { ForagingLogRankGoalConfig() },
    { ForagingLeaderboardDisplayConfig() }
) {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the foraging collection leaderboard on Galatea, The Park, or the Hub.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigLink(owner = ForagingLeaderboardConfig::class, field = "enabled")
    val position: Position = Position(10, 10)

    @Expose
    @ConfigOption(name = "Debug", desc = "Debug options for troubleshooting leaderboard rendering.")
    @Accordion
    val debug: ForagingLeaderboardDebugConfig = ForagingLeaderboardDebugConfig()
}

class ForagingLeaderboardDisplayConfig : EliteDisplayGenericConfig()

class ForagingLeaderboardDebugConfig {
    @Expose
    @ConfigOption(name = "Force Display", desc = "Show the foraging leaderboard even without breaking a tree.")
    @ConfigEditorBoolean
    var forceDisplay: Boolean = false

    @Expose
    @ConfigOption(name = "Forced Log Type", desc = "Log type to use when Force Display is enabled.")
    @ConfigEditorDropdown
    val forcedLogType: Property<ForagingLogType> = Property.of(ForagingLogType.OAK)

    @Expose
    @ConfigOption(name = "Verbose Logging", desc = "Write foraging leaderboard debug information to logs.")
    @ConfigEditorBoolean
    var verboseLogging: Boolean = false
}

class ForagingLogRankGoalConfig : MultiTypeRankGoalConfig<ForagingLogType, ForagingLogTypeRankGoalsConfig>(
    { ForagingLogTypeRankGoalsConfig() }
) {
    @Expose
    @ConfigOption(
        name = "Rank Goal",
        desc = "What log types to set a custom rank goal for. Applies to all leaderboard modes."
    )
    @ConfigEditorDraggableList
    override val rankGoalTypes: Property<MutableList<ForagingLogType>> = Property.of(mutableListOf())
}
