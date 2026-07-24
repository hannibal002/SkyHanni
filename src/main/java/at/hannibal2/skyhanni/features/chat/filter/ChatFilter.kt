package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.utils.RegexUtils.matches
import java.util.regex.Pattern

@FunctionalInterface
interface ChatFilter {
    /**
     * Return a reason to block.
     */
    fun block(message: String): String?
}

abstract class RegexChatFilter protected constructor(
    private val reason: String,
) : ChatFilter {

    init {
        require(reason.none { it.isWhitespace() || it.isUpperCase() || it == '-' }) {
            "Reason must be lowercase and without whitespace or dashes."
        }
    }

    protected open fun isEnabled(): Boolean = true

    protected abstract val patterns: List<Pattern>

    override fun block(message: String): String? =
        if (patterns.matches(message)) reason else null
}
