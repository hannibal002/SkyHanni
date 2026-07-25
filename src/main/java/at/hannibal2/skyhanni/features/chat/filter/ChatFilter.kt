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

interface ConfigChatFilter : ChatFilter {
    fun registerConfig(
        config: Property<Boolean>,
        onEnable: (() -> Unit) = { CoreChatFilter.add(this) },
        onDisable: (() -> Unit) = { CoreChatFilter.remove(this) }
    ) {
        config.whenChanged { _, new ->
            if (new) onEnable()
            else onDisable()
        }
        if (config.get()) onEnable() else onDisable()
    }
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
    config: Property<Boolean>,
) : AbstractRegexChatFilter(reason), ConfigChatFilter {

    init {
        registerConfig(config)
    }
}

abstract class RegexIslandChatFilter(
    reason: String,
    config: Property<Boolean>,
    detector: IslandDetector,
) : AbstractRegexChatFilter(reason), ConfigChatFilter {

    init {
        fun update() {
            if (config.get() && detector.isInside()) {
                CoreChatFilter.add(this)
            } else {
                CoreChatFilter.remove(this)
            }
        }

        registerConfig(
            config,
            onEnable = { update() },
            onDisable = { update() }
        )

        update()
    }
}
