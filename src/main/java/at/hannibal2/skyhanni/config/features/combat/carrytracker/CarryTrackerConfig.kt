package at.hannibal2.skyhanni.config.features.combat.carrytracker

import at.hannibal2.skyhanni.config.NoConfigLink
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CarryTrackerConfig {
    @ConfigOption(
        name = "Note",
        desc = "Type §e/shcarry §rto manage the carry tracker.",
    )
    @ConfigEditorInfoText
    val notice: String = ""

    @Expose
    @NoConfigLink
    val position: Position = Position(10, 10)

    @Expose
    @ConfigOption(
        name = "Suggest Carries From Trades",
        desc = "Detect coins traded and suggest adding carries based on configured prices.",
    )
    @ConfigEditorBoolean
    var suggestCarriesFromTrades: Boolean = true

    @Expose
    @ConfigOption(
        name = "Send Carry Progress To Party Chat",
        desc = "Send a message to party chat every time a carry is completed.",
    )
    @ConfigEditorBoolean
    var sendProgressToParty: Boolean = true

    @Expose
    @ConfigOption(
        name = "Auto Remove Finished Carries",
        desc = "Automatically remove finished carries without confirmation.",
    )
    @ConfigEditorBoolean
    var autoRemoveFinishedCarries: Boolean = false

    @Expose
    @ConfigOption(
        name = "Slayer Boss Spawned Notification",
        desc = "Show a notification when a customer's slayer boss spawns.",
    )
    @Accordion
    val slayerSpawnedNotification: NotificationConfig = NotificationConfig()

    @Expose
    @ConfigOption(
        name = "Carry Finished Notification",
        desc = "Show a notification when a carry is finished.",
    )
    @Accordion
    val carryFinishedNotification: NotificationConfig = NotificationConfig()

    class NotificationConfig {
        @Expose
        @ConfigOption(
            name = "Notification Chat Message",
            desc = "Send a chat message for the notification.",
        )
        @ConfigEditorBoolean
        var chat: Boolean = true

        @Expose
        @ConfigOption(
            name = "Notification Title",
            desc = "Show a title for the notification.",
        )
        @ConfigEditorBoolean
        var title: Boolean = true

        @Expose
        @ConfigOption(
            name = "Notification Sound",
            desc = "Play a sound for the notification.",
        )
        @ConfigEditorBoolean
        var sound: Boolean = true
    }
}
