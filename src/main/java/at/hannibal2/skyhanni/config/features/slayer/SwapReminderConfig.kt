package at.hannibal2.skyhanni.config.features.slayer

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.garden.pests.PestTimerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SwapReminderConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Triggers a screen alert when your active Slayer Boss reaches a set HP threshold.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "HP Threshold (%)",
        desc = "Threshold percentage to send the alert (e.g. 50%).",
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 99f, minStep = 1f)
    var hpThreshold: Int = 50

    @Expose
    @ConfigOption(
        name = "Title Text",
        desc = "Custom text shown on screen. Supports '&' color codes (e.g., &c&lROD SWAP!).",
    )
    @ConfigEditorText
    var titleText: String = "&c&lROD SWAP!"

    @Expose
    @ConfigOption(
        name = "Play Sound",
        desc = "Plays an alert sound notification.",
    )
    @ConfigEditorBoolean
    var playSound: Boolean = true

    @Expose
    @ConfigLink(owner = SwapReminderConfig::class, field = "enabled")
    val position: Position = Position(383, 93)
}
