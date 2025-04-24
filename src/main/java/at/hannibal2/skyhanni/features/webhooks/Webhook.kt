package at.hannibal2.skyhanni.features.webhooks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ApiUtils
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

private val config get() = SkyHanniMod.feature.webhook

data class Webhook(
    val url: String? = config.webhookUrl,
    val content: String? = null,
    val username: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val tts: Boolean? = null,
    var embeds: List<DiscordEmbed>? = null,
    @SerializedName("allowed_mentions") val allowedMentions: AllowedMentions? = null,
    val components: List<Any>? = null,
    @SerializedName("thread_name") val threadName: String? = null
) {
    fun sendTo(webhookUrl: String) {
        val jsonPayload = Gson().toJson(this)
        println("Sending JSON: $jsonPayload")

        ApiUtils.postJSON(webhookUrl, jsonPayload, "Discord Webhook")
    }

    fun addEmbed(embed: DiscordEmbed) {
        embeds = embeds?.plus(embed) ?: listOf(embed)
    }
}
