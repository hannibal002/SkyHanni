package at.hannibal2.skyhanni.config.features.webhooks

import at.hannibal2.skyhanni.features.webhooks.DiscordEmbed
import at.hannibal2.skyhanni.features.webhooks.Webhook
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class WebhookConfig {
    @Expose
    @ConfigOption(
        name = "Webhook URL",
        desc = "The URL of the Webhook to send messages to.\n" +
            "You can create a Webhook in a Discord server.\n" +
            "See documentation online."
    )
    @ConfigEditorText
    var webhookUrl: String = ""

    @Expose
    @ConfigOption(
        name = "Webhook Username",
        desc = "The username of the Webhook.\n" +
            "Leave empty to use the default username."
    )
    @ConfigEditorText
    var webhookUsername: String = ""

    @Expose
    @ConfigOption(
        name = "Webhook Avatar URL",
        desc = "The avatar URL of the Webhook.\n" +
            "Leave empty to use the default avatar."
    )
    @ConfigEditorText
    var webhookAvatarUrl: String = ""

    @ConfigOption(
        name = "Send Test Message",
        desc = "Send a test message to the Webhook."
    )
    @ConfigEditorButton(buttonText = "Send")
    var sendTestMessage: Runnable = Runnable {
        Webhook(
            content = "This is a test message from SkyHanni!",
            username = webhookUsername,
            avatarUrl = webhookAvatarUrl.takeIf { it.isNotEmpty() },
            embeds = listOf(
                DiscordEmbed(
                    title = "Test Embed",
                    description = "This is a test embed from SkyHanni!",
                    color = 0x00FF00,
                    timestamp = SimpleTimeMark.now().toString(),
                )
            )
        ).sendTo(webhookUrl)
    }

}
