package at.hannibal2.skyhanni.features.webhooks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ApiUtils
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

private val config get() = SkyHanniMod.feature.webhook

data class Webhook(
    val url: String = config.webhookUrl,
    val content: String? = null,
    val username: String = config.webhookUsername.takeIf { it.isNotEmpty() } ?: "SkyHanni",
    @SerializedName("avatar_url") val avatarUrl: String? = config.webhookAvatarUrl.takeIf { it.isNotEmpty() },
    val tts: Boolean? = null,
    var embeds: List<DiscordEmbed>? = null,
    @SerializedName("allowed_mentions") val allowedMentions: AllowedMentions? = null,
    val components: List<Any>? = null,
    @SerializedName("thread_name") val threadName: String? = null
) {
    fun sendTo(webhookUrl: String = config.webhookUrl) {
        if (config.onlyWhenAFK && !PlayerUtils.isAFK) {
            ChatUtils.debug("Not sending webhook because not AFK")
            return
        }

        val jsonPayload = Gson().toJson(this)
        println("Sending JSON: $jsonPayload")

        ApiUtils.postJSON(webhookUrl, jsonPayload, "Discord Webhook")
    }

    fun addEmbed(embed: DiscordEmbed): Webhook {
        embeds = embeds?.plus(embed) ?: listOf(embed)
        return this
    }
}
