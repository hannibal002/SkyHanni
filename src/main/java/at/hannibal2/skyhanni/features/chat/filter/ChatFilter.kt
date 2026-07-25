package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTag
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
    val activation: Activation
}

abstract class ChatFilterGroup {
    open val activation: Activation = Activation.Always
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
    override val activation: Activation = Activation.Config(config)
}

sealed interface Activation {
    fun isActive(): Boolean

    fun bind(onChange: (Boolean) -> Unit) {
        onChange(isActive())
    }

    fun unbind() {}

    object Always : Activation {
        override fun isActive(): Boolean = true
    }

    object Never : Activation {
        override fun isActive(): Boolean = false
    }

    class Config(
        private val property: () -> Property<Boolean>,
    ) : Activation {
        private var callback: ((Boolean) -> Unit)? = null
        override fun isActive(): Boolean = property().get()
        override fun bind(onChange: (Boolean) -> Unit) {
            callback = onChange
            val prop = property()
            prop.whenChanged { _, new ->
                callback?.invoke(new)
            }
            onChange(prop.get())
        }
        override fun unbind() {
            callback = null
        }
    }

    class Island(
        private val detector: IslandDetector,
    ) : Activation {
        constructor(islandType: IslandTypeTag) : this(IslandDetector(islandType))
        constructor(island: IslandType) : this(IslandDetector(island))

        private var callback: ((Boolean) -> Unit)? = null
        override fun isActive(): Boolean = detector.isInside()
        override fun bind(onChange: (Boolean) -> Unit) {
            callback = onChange
            onChange(detector.isInside())
            detector.register { _, _ ->
                callback?.invoke(detector.isInside())
            }
        }
        override fun unbind() {
            callback = null
        }
    }
}
