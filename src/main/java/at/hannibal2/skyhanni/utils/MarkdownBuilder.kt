package at.hannibal2.skyhanni.utils

/**
 * Adapted from NEU's DiscordMarkdownBuilder.
 * https://github.com/NotEnoughUpdates/NotEnoughUpdates/blob/master/src/main/java/io/github/moulberry/notenoughupdates/util/DiscordMarkdownBuilder.java
 */
class MarkdownBuilder {
    private val builder = StringBuilder().apply { append("```md\n") }

    fun category(name: String) = apply {
        builder.append("# $name\n")
    }

    fun append(key: String, value: Any) = apply {
        if (key.isNotEmpty()) builder.append("[$key]")
        builder.append("[$value]\n")
    }

    override fun toString(): String = builder.append("```").toString()
}
