package at.hannibal2.skyhanni.config.core.config

import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.StringUtils.addStrikethorugh
import io.github.notenoughupdates.moulconfig.observer.Property

class DependentDisplayManager<E : Enum<E>>(
    private val entries: Collection<E>,
    enabledPropProvider: () -> Property<MutableList<E>>,
    private val dependenciesOf: (E) -> Collection<E>,
    private val nameOf: (E) -> String
) {
    private var loaded = false
    private var cache: Map<E, String> = emptyMap()
    private val enabledProp by lazy { enabledPropProvider() }

    fun onConfigLoad() {
        enabledProp.afterChange { rebuildCache() }
        loaded = true
        rebuildCache()
    }

    private fun rebuildCache() {
        cache = entries.associateWith { element ->
            val missing = if (!loaded) emptySet()
            else dependenciesOf(element).filterNot { it in enabledProp.get() }
            buildString {
                val prefix = if (missing.isNotEmpty()) "§8" else ""
                var main = nameOf(element)
                if (missing.isNotEmpty()) main = main.addStrikethorugh()
                append(prefix + main)
                if (missing.isEmpty()) return@buildString
                append("  ")
                append(missing.joinToString("§7,  ") { "§c${nameOf(it)}" })
            }
        }
    }

    fun displayName(element: E) = cache[element] ?: nameOf(element)
}
