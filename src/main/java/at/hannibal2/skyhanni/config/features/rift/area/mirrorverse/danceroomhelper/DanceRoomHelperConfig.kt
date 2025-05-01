package at.hannibal2.skyhanni.config.features.rift.area.mirrorverse.danceroomhelper

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.rift.area.mirrorverse.danceroomhelper.danceroomformatting.DanceRoomFormattingConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DanceRoomHelperConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Help to solve the dance room in the Mirrorverse by showing multiple tasks at once."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Lines to Show", desc = "How many tasks you should see.")
    @ConfigEditorSlider(minStep = 1f, maxValue = 49f, minValue = 1f)
    var lineToShow: Int = 3

    @Expose
    @ConfigOption(name = "Space", desc = "Change the space between each line.")
    @ConfigEditorSlider(minStep = 1f, maxValue = 10f, minValue = -5f)
    var extraSpace: Int = 0

    @Expose
    @ConfigOption(name = "Hide Other Players", desc = "Hide other players inside the dance room.")
    @ConfigEditorBoolean
    var hidePlayers: Boolean = false

    @Expose
    @ConfigOption(
        name = "Hide Title",
        desc = "Hide Instructions, \"§aIt's happening!\" §7and \"§aKeep it up!\" §7titles."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideOriginalTitle: Boolean = false

    @Expose
    @ConfigOption(name = "Formatting", desc = "")
    @Accordion
    val danceRoomFormatting: DanceRoomFormattingConfig = DanceRoomFormattingConfig()

    @Expose
    @ConfigLink(owner = DanceRoomHelperConfig::class, field = "enabled")
    val position: Position = Position(442, 239)
}
