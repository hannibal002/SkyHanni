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

sealed interface Activation {
    fun bind(
        onEnable: () -> Unit,
        onDisable: () -> Unit,
    )
    fun unbind()

    object Always : Activation {
        private var onDisableCallback: (() -> Unit)? = null

        override fun bind(
            onEnable: () -> Unit,
            onDisable: () -> Unit,
        ) {
            onDisableCallback = onDisable
            onEnable()
        }

        override fun unbind() {
            onDisableCallback?.invoke()
            onDisableCallback = null
        }
    }

    object Never : Activation {
        override fun bind(
            onEnable: () -> Unit,
            onDisable: () -> Unit,
        ) {
        }

        override fun unbind() {}
    }

    class Config(
        private val config: Property<Boolean>,
    ) : Activation {

        private var callback: ((Boolean) -> Unit)? = null

        override fun bind(
            onEnable: () -> Unit,
            onDisable: () -> Unit,
        ) {
            callback = {
                if (it) onEnable()
                else onDisable()
            }

            config.whenChanged { _, new ->
                callback?.invoke(new)
            }
            callback?.invoke(config.get())
        }

        override fun unbind() {
            callback?.invoke(false)
            callback = null
        }
    }

    class Island(
        private val detector: IslandDetector,
    ) : Activation {

        constructor(island: IslandType) : this(IslandDetector(island))
        constructor(islandTag: IslandTypeTag) : this(IslandDetector(islandTag))

        private var onDisableCallback: (() -> Unit)? = null

        override fun bind(
            onEnable: () -> Unit,
            onDisable: () -> Unit,
        ) {
            onDisableCallback = onDisable

            if (detector.isInside()) {
                onEnable()
            } else {
                onDisable()
            }

            detector.register { _, _ ->
                if (detector.isInside()) {
                    onEnable()
                } else {
                    onDisable()
                }
            }
        }

        override fun unbind() {
            onDisableCallback?.invoke()
            onDisableCallback = null
        }
    }

    class AllOf(
        private vararg val activations: Activation,
    ) : Activation {

        private var states: BooleanArray? = null
        private var onEnableCallback: (() -> Unit)? = null
        private var onDisableCallback: (() -> Unit)? = null

        override fun bind(
            onEnable: () -> Unit,
            onDisable: () -> Unit,
        ) {
            onEnableCallback = onEnable
            onDisableCallback = onDisable

            states = BooleanArray(activations.size)

            activations.forEachIndexed { index, activation ->
                activation.bind(
                    onEnable = {
                        states?.set(index, true)
                        update()
                    },
                    onDisable = {
                        states?.set(index, false)
                        update()
                    },
                )
            }
        }

        private fun update() {
            if (states?.all { it } == true) {
                onEnableCallback?.invoke()
            } else {
                onDisableCallback?.invoke()
            }
        }

        override fun unbind() {
            activations.forEach {
                it.unbind()
            }

            onDisableCallback?.invoke()

            states = null
            onEnableCallback = null
            onDisableCallback = null
        }
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
    activationParam: Activation,
) : AbstractRegexChatFilter(reason), ActivatedChatFilter {
    constructor(reason: String, config: Property<Boolean>) : this(reason, Activation.Config(config))
    constructor(reason: String, config: Property<Boolean>, island: IslandDetector) :
        this(reason, Activation.Config(config), Activation.Island(island))
    constructor(reason: String, vararg activation: Activation) : this(reason, Activation.AllOf(*activation))

    override val activation: Activation = activationParam
}
