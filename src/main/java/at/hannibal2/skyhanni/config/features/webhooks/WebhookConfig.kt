package at.hannibal2.skyhanni.config.features.webhooks

import at.hannibal2.skyhanni.features.webhooks.DiscordEmbed
import at.hannibal2.skyhanni.features.webhooks.Webhook
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.client.Minecraft

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

    @Expose
    @ConfigOption(
        name = "Only when AFK",
        desc = "Only send messages when you are AFK."
    )
    @ConfigEditorBoolean
    var onlyWhenAFK: Boolean = false

    @Expose
    @ConfigOption(
        name = "Embed Thumbnail",
        desc = "What thumbnail to use for embeds."
    )
    @ConfigEditorDropdown
    var embedThumbnail: Property<EmbedThumbnailType> = Property.of(EmbedThumbnailType.SKIN)

    @Expose
    @ConfigOption(
        name = "Username in Embeds",
        desc = "Sends your username in the embed author field."
    )
    @ConfigEditorBoolean
    var usernameInEmbeds: Boolean = true

    @ConfigOption(
        name = "Send Test Message",
        desc = "Send a test message to the Webhook."
    )
    @ConfigEditorButton(buttonText = "Send")
    var sendTestMessage: Runnable = Runnable {
        Webhook(
            content = "This is a test message from SkyHanni!",
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

    enum class EmbedThumbnailType(
        private val displayName: String,
        private val urlProvider: () -> String?,
    ) {
        SKIN("Skin", {
            Minecraft.getMinecraft().thePlayer?.name?.let { "https://mineskin.eu/helm/$it" }
        }),
        SKYHANNI(
            "SkyHanni",
            { "https://github.com/hannibal002/SkyHanni/blob/beta/src/main/resources/assets/skyhanni/logo.png?raw=true" }
        ),
        NONE("None", { null })
        ;

        override fun toString(): String = displayName

        fun getUrl(): String? {
            return urlProvider()
        }
    }

}
