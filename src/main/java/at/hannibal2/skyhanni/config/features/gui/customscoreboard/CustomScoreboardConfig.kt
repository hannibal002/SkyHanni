package at.hannibal2.skyhanni.config.features.gui.customscoreboard

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.OnlyDevEnv
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.utils.OSUtils.openBrowser
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class CustomScoreboardConfig {

    @ConfigOption(
        name = "Removed Feature",
        desc = "SkyHanni's CustomScoreboard Has Been Removed. Please switch to the mod for more & unique features.",
    )
    @ConfigEditorButton(buttonText = "Download the Mod")
    val customScoreboardMod: Runnable = Runnable { openBrowser("https://modrinth.com/mod/skyblock-custom-scoreboard") }

    @OnlyDevEnv
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show a custom scoreboard instead of the vanilla one.")
    @ConfigEditorBoolean
    @FeatureToggle
    val enabled: Property<Boolean> = Property.of(false)

    @OnlyDevEnv
    @Expose
    @ConfigOption(name = "Appearance", desc = "Drag text to change the appearance of the advanced scoreboard.")
    @ConfigEditorDraggableList
    val scoreboardEntries: Property<MutableList<ScoreboardConfigElement>> =
        Property.of(ScoreboardConfigElement.defaultOptions.toMutableList())

    @OnlyDevEnv
    @Expose
    @ConfigOption(name = "Display Options", desc = "")
    @Accordion
    val display: DisplayConfig = DisplayConfig()

    @OnlyDevEnv
    @Expose
    @ConfigOption(name = "Background Options", desc = "")
    @Accordion
    val background: BackgroundConfig = BackgroundConfig()

    @OnlyDevEnv
    @Expose
    @ConfigOption(name = "Information Filtering", desc = "")
    @Accordion
    val informationFiltering: InformationFilteringConfig = InformationFilteringConfig()

    @OnlyDevEnv
    @Expose
    @ConfigOption(
        name = "Unknown Lines warning",
        desc = "Give a chat warning when unknown lines are found in the scoreboard.\n" +
            "§cReporting these in the Discord Server is very important, so we can know what lines are missing."
    )
    @ConfigEditorBoolean
    var unknownLinesWarning: Boolean = true

    @Expose
    @ConfigLink(owner = CustomScoreboardConfig::class, field = "enabled")
    val position: Position = Position(10, 80)
}
