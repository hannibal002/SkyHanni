package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.util.regex.Pattern

interface ChatFilter {

    /**
     * Whether this filter should even run.
     */
    fun isEnabled(): Boolean

    /**
     * Return a reason to block.
     */
    fun block(message: String): String? = null

    companion object {
        val chatFilterGroup = RepoPattern.group("chat-filter")
        val generalConfig get() = SkyHanniMod.feature.chat
        val config get() = SkyHanniMod.feature.chat.filterType
    }
}

abstract class RegexChatFilter protected constructor(
    private val reason: String,
) : ChatFilter {

    init {
        require(reason.none { it.isWhitespace() || it.isUpperCase() || it == '-' }) {
            "Reason must be lowercase and without whitespace or dashes."
        }
    }

    override fun isEnabled(): Boolean = true

    protected abstract val patterns: List<Pattern>

    override fun block(message: String): String? =
        if (patterns.matches(message)) reason else null
}
