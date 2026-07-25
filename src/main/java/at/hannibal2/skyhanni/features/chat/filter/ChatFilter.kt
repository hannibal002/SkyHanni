package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.utils.RegexUtils.matches
import io.github.notenoughupdates.moulconfig.observer.Property
import java.util.regex.Pattern

@FunctionalInterface
interface ChatFilter {
    /**
     * Return a reason to block.
     */
    fun block(message: String): String?
}

interface ActivatedChatFilter : ChatFilter {
    val activation: ChatFilterActivation
}

abstract class ChatFilterGroup {
    open val activation: ChatFilterActivation = ChatFilterActivation.Always
    abstract val filters: Set<ChatFilter>
}

abstract class AbstractRegexChatFilter(
    private val reason: String,
) : ChatFilter {
    protected abstract val patterns: List<Pattern>

    override fun block(message: String): String? =
        if (patterns.matches(message)) reason else null
}

abstract class RegexChatFilter(
    reason: String,
    config: () -> Property<Boolean>,
) : AbstractRegexChatFilter(reason), ActivatedChatFilter {
    override val activation: ChatFilterActivation = ChatFilterActivation.Config(config)
}
