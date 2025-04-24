package at.hannibal2.skyhanni.features.webhooks

import com.google.gson.annotations.SerializedName

data class DiscordEmbed(
    val title: String? = null,
    val type: String? = null, // usually "rich"
    val description: String? = null,
    val url: String? = null,
    val timestamp: String? = null,
    val color: Int? = null,
    val footer: EmbedFooter? = null,
    val image: String? = null,
    val thumbnail: String? = "https://github.com/hannibal002/SkyHanni/blob/beta/src/main/resources/assets/skyhanni/logo.png?raw=true",
    val video: String? = null,
    val provider: EmbedProvider? = null,
    val author: EmbedAuthor? = null,
    val fields: List<EmbedField>? = null
)

data class EmbedFooter(
    val text: String,
    @SerializedName("icon_url") val iconUrl: String? = null
)
data class EmbedProvider(val name: String? = null, val url: String? = null)
data class EmbedAuthor(
    val name: String,
    val url: String? = null,
    @SerializedName("icon_url") val iconUrl: String? = null
)
data class EmbedField(val name: String, val value: String, val inline: Boolean = false)
