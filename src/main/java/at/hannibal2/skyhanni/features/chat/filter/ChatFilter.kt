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

interface ChatFilterGroup {
    val filters: Set<ChatFilter>
}

interface Activation {

    fun bind(filter: ChatFilter)

    class Config(
        private val config: Property<Boolean>,
    ) : Activation {

        override fun bind(filter: ChatFilter) {
            config.whenChanged { _, enabled ->
                if (enabled) {
                    ChatFilterManager.register(filter)
                } else {
                    ChatFilterManager.unregister(filter)
                }
            }

            if (config.get()) {
                ChatFilterManager.register(filter)
            }
        }
    }

    class Island(
        private val config: Property<Boolean>,
        private val detector: IslandDetector,
    ) : Activation {

        override fun bind(filter: ChatFilter) {
            fun update() {
                if (config.get() && detector.isInside()) {
                    ChatFilterManager.register(filter)
                } else {
                    ChatFilterManager.unregister(filter)
                }
            }

            config.whenChanged { _, _ -> update() }
            detector.register { _, _ -> update() }

            update()
        }
    }
}

abstract class ConfigChatFilter(
    activation: Activation,
) : ChatFilter {

    init {
        activation.bind(this)
    }
}

abstract class RegexChatFilter(
    private val reason: String,
    activation: Activation,
) : ConfigChatFilter(activation) {

    protected abstract val patterns: List<Pattern>

    override fun block(message: String): String? =
        if (patterns.matches(message)) reason else null
}
