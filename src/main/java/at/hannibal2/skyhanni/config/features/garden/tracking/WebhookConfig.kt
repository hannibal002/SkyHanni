package at.hannibal2.skyhanni.config.features.garden.tracking

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class WebhookConfig {
    @Expose
    @ConfigOption(name = "URL", desc = "The URL of the webhook.")
    @ConfigEditorText
    var url: String = ""

    @Expose
    @ConfigOption(
        name = "Thread ID",
        desc = "If you want the message to be sent to a thread in the webhook channel put it's id here, otherwise leave blank.",
    )
    @ConfigEditorText
    var threadId: String = ""

    @Expose
    @ConfigOption(name = "Interval", desc = "The interval in minutes in which status updated will be sent.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    var interval: Int = 5
}
