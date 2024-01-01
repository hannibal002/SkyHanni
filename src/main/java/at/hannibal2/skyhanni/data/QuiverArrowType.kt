package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName

enum class QuiverArrowType(val arrow: String, val internalName: NEUInternalName) {
    NONE("None", "NONE".asInternalName()),

    SLIME_BALL("Slime Ball", "SLIME_BALL".asInternalName()),
    PRISMARINE_SHARD("Prismarine Shard", "PRISMARINE_SHARD".asInternalName()),
    FLINT("Flint Arrow", "ARROW".asInternalName()),
    REINFORCED_IRON_ARROW("Reinforced Iron Arrow", "REINFORCED_IRON_ARROW".asInternalName()),
    GOLD_TIPPED_ARROW("Gold-tipped Arrow", "GOLD_TIPPED_ARROW".asInternalName()),
    REDSTONE_TIPPED_ARROW("Redstone-tipped Arrow", "REDSTONE_TIPPED_ARROW".asInternalName()),
    EMERALD_TIPPED_ARROW("Emerald-tipped Arrow", "EMERALD_TIPPED_ARROW".asInternalName()),
    BOUNCY_ARROW("Bouncy Arrow", "BOUNCY_ARROW".asInternalName()),
    ICY_ARROW("Icy Arrow", "ICY_ARROW".asInternalName()),
    ARMORSHRED_ARROW("Armorshred Arrow", "ARMORSHRED_ARROW".asInternalName()),
    EXPLOSIVE_ARROW("Explosive Arrow", "EXPLOSIVE_ARROW".asInternalName()),
    GLUE_ARROW("Glue Arrow", "GLUE_ARROW".asInternalName()),
    NANSORB_ARROW("Nansorb Arrow", "NANSORB_ARROW".asInternalName()),
}
