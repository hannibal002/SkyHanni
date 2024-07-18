package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.Embed
import at.hannibal2.skyhanni.data.Payload
import com.google.gson.Gson

object WebhookUtils {

    var lastMessageID: Long? = 0L

    fun postPayload(payload: Payload, url: String) =
        APIUtil.postJSON(url, Gson().toJson(payload)).data.asJsonObject

    fun sendMessageToWebhook(
        webhookUrl: String,
        message: String = "",
        username: String? = null,
        avatarUrl: String? = null,
    ) {
        if (webhookUrl.isEmpty()) return LorenzDebug.log("Missing webhook url.")
        if (message.isEmpty()) return LorenzDebug.log("Missing message.")

        val messagePayload = Payload(
            content = message,
            username = username,
            avatar_url = avatarUrl,
        )

        lastMessageID = postPayload(messagePayload, webhookUrl).get("id").asLong
        return ChatUtils.debug("Message sent to webhook.")
    }

    fun sendEmbedsToWebhook(
        webhookUrl: String,
        embeds: List<Embed>,
        username: String? = null,
        avatarUrl: String? = null,
    ) {
        if (webhookUrl.isEmpty()) return LorenzDebug.log("Missing webhook url.")
        if (embeds.isEmpty()) return LorenzDebug.log("Missing embeds.")

        val embedPayload = Payload(
            embeds = embeds,
            username = username,
            avatar_url = avatarUrl,
        )

        lastMessageID = postPayload(embedPayload, webhookUrl).get("id").asLong
        return ChatUtils.debug("Embeds sent to webhook.")
    }

    fun editMessageEmbeds(
        webhookUrl: String,
        embeds: List<Embed>,
        username: String? = null,
        avatarUrl: String? = null,
    ) {
        if (webhookUrl.isEmpty()) return LorenzDebug.log("Missing webhook url.")
        if (embeds.isEmpty()) return LorenzDebug.log("Missing message.")

        val messagePayload = Payload(
            embeds = embeds,
            username = username,
            avatar_url = avatarUrl,
        )

        lastMessageID = if (lastMessageID == null) {
            postPayload(messagePayload, webhookUrl).get("id").asLong
        } else {
            postPayload(messagePayload, "$webhookUrl/messages/$lastMessageID").get("id").asLong
        }

        ChatUtils.debug("Embeds edited.")
    }
}
