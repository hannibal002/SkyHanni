package at.hannibal2.skyhanni.data

data class Payload(
    val content: String? = "",
    val tts: Boolean? = false,
    val embeds: List<Embed>? = null,
    val username: String? = null,
    val avatarUrl: String? = null,
)

data class Embed(
    val title: String,
    val color: Int,
    val fields: List<Field>,
    val timestamp: String,
    val thumbnail: Thumbnail,
    val footer: Footer,
)

data class Field(
    val name: String,
    val value: String,
    val inline: Boolean,
)

data class Thumbnail(val url: String)

data class Footer(val text: String)
