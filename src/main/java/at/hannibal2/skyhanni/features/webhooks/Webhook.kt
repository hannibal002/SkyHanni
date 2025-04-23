package at.hannibal2.skyhanni.features.webhooks

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class Webhook(
    val content: String? = null,
    val username: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val tts: Boolean = false,
    val embeds: List<DiscordEmbed>? = null,
    @SerializedName("allowed_mentions") val allowedMentions: AllowedMentions? = null,
    val components: List<Any>? = null,
    @SerializedName("thread_name") val threadName: String? = null
) {
    fun sendTo(webhookUrl: String) {
        val jsonPayload = Gson().toJson(this)
        val url = URL(webhookUrl)

        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true

            OutputStreamWriter(outputStream).use { it.write(jsonPayload) }

            if (responseCode !in 200..299) {
                System.err.println("Discord webhook failed: $responseCode $responseMessage")
            }
        }
    }
}
