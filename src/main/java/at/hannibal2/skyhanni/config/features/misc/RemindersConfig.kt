package at.hannibal2.skyhanni.config.features.misc

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
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
    @ConfigEditorSlider(minValue = 0f, maxValue = 60f, minStep = 1f)
    var interval: Float = 5f
}
