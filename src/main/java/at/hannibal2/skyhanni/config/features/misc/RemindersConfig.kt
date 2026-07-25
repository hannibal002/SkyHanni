package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class RemindersConfig {
    @Expose
    @ConfigOption(
        name = "Auto Delete Reminders",
        desc = "Automatically deletes reminders after they have been shown once."
    )
    @ConfigEditorBoolean
    var autoDeleteReminders: Boolean = false

    @Expose
    @ConfigOption(
        name = "Reminder Interval",
        desc = "The interval in minutes in which reminders are shown again, after they have been shown once."
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 60f, minStep = 1f)
    var interval: Float = 5f

    @Expose
    @ConfigOption(name = "Show Title", desc = "Show a title on screen when a reminder is due.")
    @ConfigEditorBoolean
    var showTitle: Boolean = true

    @Expose
    @ConfigOption(
        name = "Show Reminders HUD",
        desc = "Display active reminders on screen with time remaining."
    )
    @ConfigEditorBoolean
    var showHud: Boolean = false

    @Expose
    @ConfigLink(owner = RemindersConfig::class, field = "showHud")
    val hudPosition: Position = Position(10, -130)
}
