package at.hannibal2.skyhanni.config.features.webhooks

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class NotificationsConfig {
    @Expose
    @ConfigOption(
        name = "Party Invite Notification",
        desc = "Send a notification when you receive a party invite."
    )
    @ConfigEditorBoolean
    var partyInviteNotification: Boolean = false

    @Expose
    @ConfigOption(
        name = "Disconnect Notification",
        desc = "Send a notification when you disconnect from the server."
    )
    @ConfigEditorBoolean
    var disconnectNotification: Boolean = false
}
