package at.hannibal2.skyhanni.features.misc.items.enchants

import net.minecraft.network.chat.Component

class EnchantCache {
    var cachedLoreBefore: List<Component> = listOf()
    var cachedLoreAfter: List<Component> = listOf()

    // So tooltip gets changed on the same item if the config was changed in the interim
    var configChanged = false

    fun updateBefore(loreBeforeModification: List<Component>) {
        cachedLoreBefore = loreBeforeModification.toList()
    }

    fun updateAfter(loreAfterModification: List<Component>) {
        cachedLoreAfter = loreAfterModification.toList()
        configChanged = false
    }

    fun isCached(loreBeforeModification: List<Component>): Boolean = !configChanged && loreBeforeModification == cachedLoreBefore
}
