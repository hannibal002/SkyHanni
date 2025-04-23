package at.hannibal2.skyhanni.features.webhooks

data class DiscordEmbed(
    val title: String? = null,
    val type: String? = null, // usually "rich"
    val description: String? = null,
    val url: String? = null,
    val timestamp: String? = null,
    val color: Int? = null,
    val footer: EmbedFooter? = null,
    val image: EmbedImage? = null,
    val thumbnail: EmbedThumbnail? = null,
    val video: EmbedVideo? = null,
    val provider: EmbedProvider? = null,
    val author: EmbedAuthor? = null,
    val fields: List<EmbedField>? = null
)

data class EmbedFooter(val text: String, val iconUrl: String? = null)
data class EmbedImage(val url: String)
data class EmbedThumbnail(val url: String)
data class EmbedVideo(val url: String)
data class EmbedProvider(val name: String? = null, val url: String? = null)
data class EmbedAuthor(val name: String, val url: String? = null, val iconUrl: String? = null)
data class EmbedField(val name: String, val value: String, val inline: Boolean = false)
