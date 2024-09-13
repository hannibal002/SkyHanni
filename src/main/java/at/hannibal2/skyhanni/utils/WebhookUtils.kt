package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.Embed
import at.hannibal2.skyhanni.data.Payload
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.Gson

@SkyHanniModule
object WebhookUtils {

    private const val SKYHANNI_URL = "https://github.com/hannibal002/SkyHanni/blob/beta/src/main/resources/assets/skyhanni/logo.png?raw=true"

    private var lastMessageID: Long? = null

    private val patternGroup = RepoPattern.group("utils.webhook")

    /**
     * REGEX-TEST: https://discord.com/api/webhooks/1263107706752073801/EIjM9xNoZJmZ3zn4QcIIpGTlrg6xDH8BgHxYRyjR5VbFv8_6ql1UEijyBs7SDFlv8SCB
     * REGEX-TEST: https://discord.com/api/webhooks/1263107706752073801/EIjM9xNoZJmZ3zn4QcIIpGTlrg6xDH8BgHxYRyjR5VbFv8_6ql1UEijyBs7SDFlv8SCB?thread_id=1264540140576178200
     * REGEX-TEST: https://discord.com/api/webhooks/1263107706752073801/EIjM9xNoZJmZ3zn4QcIIpGTlrg6xDH8BgHxYRyjR5VbFv8_6ql1UEijyBs7SDFlv8SCB/messages/1264323904462389385
     * REGEX-TEST: https://discord.com/api/webhooks/1263107706752073801/EIjM9xNoZJmZ3zn4QcIIpGTlrg6xDH8BgHxYRyjR5VbFv8_6ql1UEijyBs7SDFlv8SCB/messages/1264323904462389385?wait=true
     */
    private val webhookPattern by patternGroup.pattern(
        "webhook",
        "^https:\\/\\/discord\\.com\\/api\\/webhooks\\/\\d+\\/[^\\/?\\s]+(?:\\/messages\\/\\d+)?(?:\\?thread_id=\\d+)?(?:[?&]wait=true)?$",
    )

    private fun convertToWebhook(webhookUrl: String, threadID: String?, edit: Boolean, wait: Boolean) : String {
        var finalUrl = if (lastMessageID != null && edit) "$webhookUrl/messages/$lastMessageID" else webhookUrl

        val queryParams = listOfNotNull(
            threadID?.let { "thread_id=$it" },
            if (wait) "wait=true" else null
        ).takeIf { it.isNotEmpty() }?.joinToString("&")

        if (!queryParams.isNullOrEmpty()) {
            finalUrl += "?$queryParams"
        }

        return finalUrl
    }

    private fun checkIfWebhookValid(webhookUrl: String): Boolean {
        return when {
            webhookUrl.isEmpty() || webhookUrl == "?wait=true" -> {
                LorenzDebug.log("Missing webhook url.")
                false
            }

            !webhookPattern.matches(webhookUrl) -> {
                ChatUtils.debug("Webhook url is invalid -> $webhookUrl")
                false
            }

            else -> true
        }
    }

    private fun checkAndCreateMessagePayload(
        finalUrl: String,
        message: String,
        username: String?,
        avatarUrl: String?,
    ): Payload? {
        if (!checkIfWebhookValid(finalUrl)) return null
        if (message.isEmpty()) return null

        return Payload(
            content = message,
            username = username,
            avatar_url = avatarUrl,
        )
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

    private fun checkAndCreateEmbedPayload(
        finalUrl: String,
        embeds: List<Embed>,
        username: String?,
        avatarUrl: String?,
    ): Payload? {
        if (!checkIfWebhookValid(finalUrl)) return null
        if (embeds.isEmpty()) return null
        if (checkForEmptyEmbeds(embeds)) return null

        return Payload(
            embeds = embeds,
            username = username,
            avatar_url = avatarUrl,
        )
    }

    fun postPayload(payload: Payload, url: String) =
        APIUtil.postJSON(url, Gson().toJson(payload)).data.asJsonObject

    fun patchPayload(payload: Payload, url: String) =
        APIUtil.patchJSON(url, Gson().toJson(payload)).data.asJsonObject

    fun sendMessageToWebhook(
        webhookUrl: String,
        message: String = "",
        threadID: String? = null,
        username: String? = null,
        avatarUrl: String? = SKYHANNI_URL,
        wait: Boolean = true,
    ): Boolean {
        val finalUrl = convertToWebhook(webhookUrl, threadID, false, wait)

        val messagePayload = checkAndCreateMessagePayload(finalUrl, message, username, avatarUrl) ?: return false

        val response = postPayload(messagePayload, finalUrl)
        if (response.has("id")) lastMessageID = response.get("id").asLong

        ChatUtils.debug("Message sent to webhook.")
        return true
    }

    fun sendEmbedsToWebhook(
        webhookUrl: String,
        embeds: List<Embed>,
        threadID: String? = null,
        username: String? = null,
        avatarUrl: String? = SKYHANNI_URL,
        wait: Boolean = true,
    ): Boolean {
        val finalUrl = convertToWebhook(webhookUrl, threadID, false, wait)
        val embedPayload = checkAndCreateEmbedPayload(finalUrl, embeds, username, avatarUrl) ?: return false

        val response = postPayload(embedPayload, finalUrl)
        lastMessageID = if (response.has("id")) response.get("id").asLong else null

        ChatUtils.debug("Embeds sent to webhook.")
        return true
    }

    fun editMessageEmbeds(
        webhookUrl: String,
        embeds: List<Embed>,
        threadID: String? = null,
        username: String? = null,
        avatarUrl: String? = SKYHANNI_URL,
        wait: Boolean = true,
    ): Boolean {
        val edit = if (lastMessageID != null) true else false
        val finalUrl = convertToWebhook(webhookUrl, threadID, edit, wait)
        val embedPayload = checkAndCreateEmbedPayload(finalUrl, embeds, username, avatarUrl) ?: return false

        val response = if (edit) patchPayload(embedPayload, finalUrl) else postPayload(embedPayload, finalUrl)
        lastMessageID = if (response.has("id")) response.get("id").asLong else null

        if (edit) ChatUtils.debug("Embeds edited.") else ChatUtils.debug("Embeds sent to webhook.")
        return true
    }
}
