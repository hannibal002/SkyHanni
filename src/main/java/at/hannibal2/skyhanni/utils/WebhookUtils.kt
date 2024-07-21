package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.Embed
import at.hannibal2.skyhanni.data.Payload
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.Gson

@SkyHanniModule
object WebhookUtils {

    private var lastMessageID: Long? = null

    private val patternGroup = RepoPattern.group("utils.webhook")

    /**
     * REGEX-TEST: https://discord.com/api/webhooks/1263107706752073801/EIjM9xNoZJmZ3zn4QcIIpGTlrg6xDH8BgHxYRyjR5VbFv8_6ql1UEijyBs7SDFlv8SCB
     * REGEX-TEST: https://discord.com/api/webhooks/1263107706752073801/EIjM9xNoZJmZ3zn4QcIIpGTlrg6xDH8BgHxYRyjR5VbFv8_6ql1UEijyBs7SDFlv8SCB?wait=true
     * REGEX-TEST: https://discord.com/api/webhooks/1263107706752073801/EIjM9xNoZJmZ3zn4QcIIpGTlrg6xDH8BgHxYRyjR5VbFv8_6ql1UEijyBs7SDFlv8SCB/messages/1264323904462389385
     * REGEX-TEST: https://discord.com/api/webhooks/1263107706752073801/EIjM9xNoZJmZ3zn4QcIIpGTlrg6xDH8BgHxYRyjR5VbFv8_6ql1UEijyBs7SDFlv8SCB/messages/1264323904462389385?wait=true
     */
    private val webhookPattern by patternGroup.pattern(
        "webhook",
        "^https:\\/\\/discord\\.com\\/api\\/webhooks\\/\\d+\\/[^\\/?\\s]+(?:\\/messages\\/\\d+)?(?:\\?wait=true)?\$",
    )

    private fun checkIfWebhookValid(webhookUrl: String): MutableList<String>? {
        return mutableListOf<String>().apply {
            if (webhookUrl.isEmpty()) add("Missing webhook url.")
            if (!webhookPattern.matches(webhookUrl)) add("Webhook url is invalid.")
        }.takeIf { it.isNotEmpty() }
    }

    private fun checkForEmptyEmbeds(embeds: List<Embed>): Boolean =
        if (embeds.any { embed ->
                embed.fields.filter { it.value.isEmpty() }
                    .also { emptyFields ->
                        emptyFields.forEach { field ->
                            LorenzDebug.log("Field ${field.name} has empty value ${field.value}")
                        }
                    }
                    .isNotEmpty()
            }) {
            true
        } else false

    fun postPayload(payload: Payload, url: String) =
        APIUtil.postJSON(url, Gson().toJson(payload)).data.asJsonObject

    fun patchPayload(payload: Payload, url: String) =
        APIUtil.patchJSON(url, Gson().toJson(payload)).data.asJsonObject

    fun sendMessageToWebhook(
        webhookUrl: String,
        message: String = "",
        username: String? = null,
        avatarUrl: String? = null,
        wait: Boolean = true,
    ) {
        val finalUrl = if (wait) "$webhookUrl?wait=true" else webhookUrl

        checkIfWebhookValid(finalUrl)?.forEach { LorenzDebug.log(it) }
        if (message.isEmpty()) return LorenzDebug.log("Missing message.")

        val messagePayload = Payload(
            content = message,
            username = username,
            avatar_url = avatarUrl,
        )

        val response = postPayload(messagePayload, finalUrl)
        if (response.has("id")) lastMessageID = response.get("id").asLong

        ChatUtils.debug("Message sent to webhook.")
    }

    fun sendEmbedsToWebhook(
        webhookUrl: String,
        embeds: List<Embed>,
        username: String? = null,
        avatarUrl: String? = null,
        wait: Boolean = true,
    ) {
        val finalUrl = if (wait) "$webhookUrl?wait=true" else webhookUrl

        checkIfWebhookValid(finalUrl)?.forEach { LorenzDebug.log(it) }
        if (embeds.isEmpty()) return LorenzDebug.log("Missing embeds.")
        if (checkForEmptyEmbeds(embeds)) return LorenzDebug.log("Some fields are empty.")

        val embedPayload = Payload(
            embeds = embeds,
            username = username,
            avatar_url = avatarUrl,
        )

        val response = postPayload(embedPayload, finalUrl)
        lastMessageID = if (response.has("id")) response.get("id").asLong else null

        ChatUtils.debug("Embeds sent to webhook.")
    }

    fun editMessageEmbeds(
        webhookUrl: String,
        embeds: List<Embed>,
        username: String? = null,
        avatarUrl: String? = null,
        wait: Boolean = true,
    ) {
        var finalUrl = if (lastMessageID != null) "$webhookUrl/messages/$lastMessageID" else webhookUrl
        if (wait) finalUrl += "?wait=true"

        checkIfWebhookValid(finalUrl)?.forEach { LorenzDebug.log(it) }
        if (embeds.isEmpty()) return LorenzDebug.log("Missing message.")
        if (checkForEmptyEmbeds(embeds)) return LorenzDebug.log("Some fields are empty.")

        val embedPayload = Payload(
            embeds = embeds,
            username = username,
            avatar_url = avatarUrl,
        )

        val response = if (lastMessageID != null) patchPayload(embedPayload, finalUrl) else postPayload(embedPayload, finalUrl)
        lastMessageID = if (response.has("id")) response.get("id").asLong else null
        if (lastMessageID != null) ChatUtils.debug("Embeds edited.") else ChatUtils.debug("Embeds sent to webhook.")
    }
}
