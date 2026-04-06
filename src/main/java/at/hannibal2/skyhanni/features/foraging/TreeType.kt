package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName

enum class TreeType(private val displayName: String) {
    FIG("Fig"),
    MANGROVE("Mangrove"),
    ;

    override fun toString() = displayName

    fun getBaseLog() = internalNameCache.getOrPut((this to false)) { "${name}_LOG".toInternalName() }
    fun getEnchantedLog() = internalNameCache.getOrPut((this to true)) { "ENCHANTED_${name}_LOG".toInternalName() }

    companion object {
        private val internalNameCache: MutableMap<Pair<TreeType, Boolean>, NeuInternalName> = mutableMapOf()
        fun byNameOrNull(name: String): TreeType? = TreeType.entries.find {
            it.name.equals(name, ignoreCase = true)
        }
    }
}
