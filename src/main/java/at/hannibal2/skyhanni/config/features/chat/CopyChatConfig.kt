package at.hannibal2.skyhanni.config.features.chat

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.JsonObject
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class CopyChatConfig {
    @Expose
    @ConfigOption(
        name = "Copy Chat",
        desc = "Right click a chat message to copy it. Holding Shift will copy the message with " +
            "Shwords applied, and holding Ctrl will copy only one line.",
    )
    @SearchTag("control")
    @ConfigEditorBoolean
    @FeatureToggle
    var copyChat: Boolean = false

    @Expose
    @ConfigOption(
        name = "Copy modified messages by default ",
        desc = "Swaps the Shift+Click and default Logic of copy chat.",
    )
    @ConfigEditorBoolean
    var copyFormattedMessage: Boolean = false

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(134, "chat.copyChat") { element ->
            JsonObject().apply {
                add("copyChat", element)
                addProperty("copyFormattedMessage", false)
            }
        }
    }
}
