package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTag
import io.github.notenoughupdates.moulconfig.observer.Property

sealed interface ChatFilterActivation {
    fun bind(onChange: (Boolean) -> Unit)
    fun unbind() {}

    object Always : ChatFilterActivation {
        override fun bind(onChange: (Boolean) -> Unit) {
            onChange(true)
        }
    }

    object Never : ChatFilterActivation {
        override fun bind(onChange: (Boolean) -> Unit) {
            onChange(false)
        }
    }

    class Config(
        private val property: () -> Property<Boolean>,
    ) : ChatFilterActivation {
        private var callback: ((Boolean) -> Unit)? = null
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
    ) : ChatFilterActivation {
        constructor(islandType: IslandTypeTag) : this(IslandDetector(islandType))
        constructor(island: IslandType) : this(IslandDetector(island))

        private var callback: ((Boolean) -> Unit)? = null
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

    class AllOf(
        private val activations: List<ChatFilterActivation>,
    ) : ChatFilterActivation {
        constructor(vararg activations: ChatFilterActivation) : this(activations.toList())

        private val actives = Array(activations.size) { false }
        private var activeAmount = 0

        private var callback: ((Boolean) -> Unit)? = null
        override fun bind(onChange: (Boolean) -> Unit) {
            callback = onChange
            activations.withIndex().forEach { (index, activation) ->
                activation.bind { active ->
                    if (actives[index] != active) {
                        activeAmount += if (active) 1 else -1
                        actives[index] = active
                    }
                    callback?.invoke(isActive())
                }
            }
        }

        override fun unbind() {
            callback = null
            activations.forEach { it.unbind() }
            activeAmount = 0
            actives.fill(false)
        }

        fun isActive(): Boolean = activeAmount == activations.size
    }
}
