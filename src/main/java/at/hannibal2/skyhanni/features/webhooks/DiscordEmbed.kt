package at.hannibal2.skyhanni.features.webhooks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import com.google.gson.annotations.SerializedName

private val config get() = SkyHanniMod.feature.webhook

data class DiscordEmbed(
    val title: String? = null,
    val type: String? = null, // usually "rich"
    val description: String? = null,
    val url: String? = null,
    val timestamp: String? = null,
    val color: Int? = null,
    val footer: EmbedFooter? = null,
    val image: EmbedImage? = null,
    val thumbnail: EmbedThumbnail? = EmbedThumbnail(config.embedThumbnail.get().getUrl().orEmpty()),
    val video: EmbedVideo? = null,
    val provider: EmbedProvider? = null,
    val author: EmbedAuthor? = EmbedAuthor(
        name = if (config.usernameInEmbeds) {
            MinecraftCompat.localPlayer.name
        } else {
            ""
        },
    ),
    val fields: List<EmbedField>? = null
)

data class EmbedFooter(
    val text: String,
    @SerializedName("icon_url") val iconUrl: String? = null
)
data class EmbedProvider(val name: String? = null, val url: String? = null)
data class EmbedImage(val url: String)
data class EmbedThumbnail(val url: String)
data class EmbedVideo(val url: String)
data class EmbedAuthor(
    val name: String,
    val url: String? = null,
    @SerializedName("icon_url") val iconUrl: String? = null
)
data class EmbedField(val name: String, val value: String, val inline: Boolean = false)
