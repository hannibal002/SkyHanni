package at.hannibal2.skyhanni.features.combat.endernodetracker

import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName

enum class EnderNode(
    val internalName: NEUInternalName,
    val displayName: String,
) {

    ENCHANTED_ENDSTONE("ENCHANTED_ENDSTONE".asInternalName(), "§aEnchanted End Stone"),
    ENCHANTED_OBSIDIAN("ENCHANTED_OBSIDIAN".asInternalName(), "§aEnchanted Obsidian"),
    ENCHANTED_ENDER_PEARL("ENCHANTED_ENDER_PEARL".asInternalName(), "§aEnchanted Ender Pearl"),
    GRAND_EXP_BOTTLE("GRAND_EXP_BOTTLE".asInternalName(), "§aGrand Experience Bottle"),
    TITANIC_EXP_BOTTLE("TITANIC_EXP_BOTTLE".asInternalName(), "§9Titanic Experience Bottle"),
    END_STONE_SHULKER("END_STONE_SHULKER".asInternalName(), "§9End Stone Shulker"),
    ENDSTONE_GEODE("ENDSTONE_GEODE".asInternalName(), "§9End Stone Geode"),
    MAGIC_RUNE("MAGIC_RUNE;1".asInternalName(), "§d◆ Magical Rune I"),
    ENDER_GAUNTLET("ENDER_GAUNTLET".asInternalName(), "§5Ender Gauntlet"),
    MITE_GEL("MITE_GEL".asInternalName(), "§5Mite Gel"),
    SHRIMP_THE_FISH("SHRIMP_THE_FISH".asInternalName(), "§cShrimp the Fish"),

    END_HELMET("END_HELMET".asInternalName(), "§5Ender Helmet"),
    END_CHESTPLATE("END_CHESTPLATE".asInternalName(), "§5Ender Chestplate"),
    END_LEGGINGS("END_LEGGINGS".asInternalName(), "§5Ender Leggings"),
    END_BOOTS("END_BOOTS".asInternalName(), "§5Ender Boots"),
    ENDER_NECKLACE("ENDER_NECKLACE".asInternalName(), "§5Ender Necklace"),
    COMMON_ENDERMAN_PET("ENDERMAN;0".asInternalName(), "§fEnderman"),
    UNCOMMON_ENDERMAN_PET("ENDERMAN;1".asInternalName(), "§aEnderman"),
    RARE_ENDERMAN_PET("ENDERMAN;2".asInternalName(), "§9Enderman"),
    EPIC_ENDERMAN_PET("ENDERMAN;3".asInternalName(), "§5Enderman"),
    LEGENDARY_ENDERMAN_PET("ENDERMAN;4".asInternalName(), "§6Enderman")
}
