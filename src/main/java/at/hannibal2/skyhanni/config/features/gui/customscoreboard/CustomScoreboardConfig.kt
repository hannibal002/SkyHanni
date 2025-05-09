package at.hannibal2.skyhanni.config.features.gui.customscoreboard

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
//#if MC < 1.21
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardConfigElement
//#endif
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

// todo 1.21 impl needed
class CustomScoreboardConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show a custom scoreboard instead of the vanilla one.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Property<Boolean> = Property.of(false)

    //#if MC < 1.21
    @Expose
    @ConfigOption(name = "Appearance", desc = "Drag text to change the appearance of the advanced scoreboard.")
    @ConfigEditorDraggableList
    var scoreboardEntries: Property<MutableList<ScoreboardConfigElement>> =
        Property.of(ScoreboardConfigElement.defaultOptions.toMutableList())

    @ConfigOption(name = "Reset Appearance", desc = "Reset the appearance of the advanced scoreboard.")
    @ConfigEditorButton(buttonText = "Reset")
    var reset: Runnable = Runnable(CustomScoreboard::resetAppearance)
    //#endif

    @Expose
    @ConfigOption(name = "Display Options", desc = "")
    @Accordion
    var display: DisplayConfig = DisplayConfig()

    @Expose
    @ConfigOption(name = "Background Options", desc = "")
    @Accordion
    var background: BackgroundConfig = BackgroundConfig()

    @Expose
    @ConfigOption(name = "Information Filtering", desc = "")
    @Accordion
    var informationFiltering: InformationFilteringConfig = InformationFilteringConfig()

    @Expose
    @ConfigOption(
        name = "Unknown Lines warning",
        desc = "Give a chat warning when unknown lines are found in the scoreboard.\n" +
            "§cReporting these in the Discord Server are very important, so we can know what lines are missing."
    )
    @ConfigEditorBoolean
    var unknownLinesWarning: Boolean = true

    @Expose
    @ConfigLink(owner = CustomScoreboardConfig::class, field = "enabled")
    var position: Position = Position(10, 80)
}
