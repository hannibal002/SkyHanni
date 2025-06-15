package at.hannibal2.skyhanni.config.features.garden.laneswitch

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class LaneSwitchNotificationConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Send a notification when approaching the end of a lane and you should switch lanes.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Seconds Before",
        desc = "How many seconds before reaching the end of the lane should the warning happen?",
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    var secondsBefore: Int = 5

    @Expose
    @ConfigOption(name = "Text", desc = "The text with color to be displayed as the notification.")
    @ConfigEditorText
    var text: String = "&eLane Switch incoming."

    @Expose
    @ConfigOption(name = "Sound Settings", desc = "")
    @Accordion
    val sound: LaneSwitchSoundSettings = LaneSwitchSoundSettings()
}
