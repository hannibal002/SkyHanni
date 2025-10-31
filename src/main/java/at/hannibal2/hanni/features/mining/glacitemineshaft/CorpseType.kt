package at.hannibal2.hanni.features.mining.glacitemineshaft

import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName

enum class CorpseType(val displayName: String, key: String? = null) {
    LAPIS("§9Lapis"),
    TUNGSTEN("§7Tungsten", "TUNGSTEN_KEY"),
    UMBER("§6Umber", "UMBER_KEY"),
    VANGUARD("§fVanguard", "SKELETON_KEY"),
    ;

    val key = key?.toInternalName()

    override fun toString(): String = displayName
}
