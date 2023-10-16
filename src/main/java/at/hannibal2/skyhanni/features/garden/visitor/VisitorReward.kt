package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack

enum class VisitorReward(private val rawInternalName: String) {
    FLOWERING_BOUQUET("FLOWERING_BOUQUET"),
    OVERGROWN_GRASS("OVERGROWN_GRASS"),
    GREEN_BANDANA("GREEN_BANDANA"),
    DEDICATION("DEDICATION;4"),
    MUSIC_RUNE("MUSIC_RUNE;1"),
    SPACE_HELMET("DCTR_SPACE_HELM"),
    CULTIVATING("CULTIVATING;1"),
    REPLENISH("REPLENISH;1"),
    ;

    private val internalName by lazy { rawInternalName.asInternalName() }
    val itemStack by lazy { internalName.getItemStack() }
    val displayName by lazy { itemStack.nameWithEnchantment ?: internalName.toString() }

    companion object {
        fun getByInternalName(internalName: NEUInternalName) = entries.firstOrNull { it.internalName == internalName }
    }
}