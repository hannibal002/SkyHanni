package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class BitsConfig {
    @Expose
    @ConfigOption(
        name = "Bulk Buy Cookie Time",
        desc = "Corrects the time for cookies if bought in bulk on the buy item."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var bulkBuyCookieTime: Boolean = true

    @Expose
    @ConfigOption(name = "Bits on Cookie", desc = "Show the bits you would gain on a cookies.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showBitsOnCookie: Boolean = true

    @Expose
    @ConfigOption(name = "Bits on Cookie Change", desc = "Show the change in available bits on cookies.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showBitsChangeOnCookie: Boolean = false

    @Expose
    @ConfigOption(name = "Enable No Bits Warning", desc = "Alerts you when you have no bits available.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enableWarning: Boolean = true

    @Expose
    @ConfigOption(name = "Notification Sound", desc = "Play a notification sound when you get a warning.")
    @ConfigEditorBoolean
    var notificationSound: Boolean = true

    @Expose
    @ConfigOption(name = "Bits Gain Chat Message", desc = "Show a chat message when you gain bits.")
    @ConfigEditorBoolean
    @FeatureToggle
    var bitsGainChatMessage: Boolean = true

    @Expose
    @ConfigOption(name = "Message Threshold", desc = "The amount of bits you need to get to show the message.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 1000f, minStep = 1f)
    var messageThreshold: Int = 400
}
