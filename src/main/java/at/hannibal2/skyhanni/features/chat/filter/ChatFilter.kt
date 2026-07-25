package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
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
        onEnable: (() -> Unit) = { ChatFilterManager.register(this) },
        onDisable: (() -> Unit) = { ChatFilterManager.unregister(this) }
    ) {
        config.whenChanged { _, new ->
            if (new) onEnable()
            else onDisable()
        }
        registeredFilters?.add(config) ?: run {
            if (config.get()) onEnable()
        }
    }

    @SkyHanniModule
    companion object {
        private var registeredFilters: MutableSet<Property<Boolean>>? = mutableSetOf()
        fun onConfigLoad(event: ConfigLoadEvent) {
            registeredFilters?.forEach { config ->
                if (config.get()) {
                    config.notifyObservers()
                }
            }
            registeredFilters = null
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
                ChatFilterManager.register(this)
            } else {
                ChatFilterManager.unregister(this)
            }
        }

        detector.register { _, _ -> update() }
        registerConfig(
            config,
            onEnable = { update() },
            onDisable = { update() }
        )
    }
}
