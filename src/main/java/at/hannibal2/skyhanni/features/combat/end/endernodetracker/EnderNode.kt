package at.hannibal2.skyhanni.features.combat.end.endernodetracker

import at.hannibal2.skyhanni.config.features.combat.end.EnderNodeConfig
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName

enum class EnderNode(
    val internalName: NeuInternalName,
    val displayName: String,
    private val dropType: NodeDropType = NodeDropType.MISC,
) {
    ENCHANTED_ENDSTONE("ENCHANTED_ENDSTONE".toInternalName(), "§aEnchanted End Stone"),
    ENCHANTED_OBSIDIAN("ENCHANTED_OBSIDIAN".toInternalName(), "§aEnchanted Obsidian"),
    ENCHANTED_ENDER_PEARL("ENCHANTED_ENDER_PEARL".toInternalName(), "§aEnchanted Ender Pearl"),
    GRAND_EXP_BOTTLE("GRAND_EXP_BOTTLE".toInternalName(), "§aGrand Experience Bottle"),
    TITANIC_EXP_BOTTLE("TITANIC_EXP_BOTTLE".toInternalName(), "§9Titanic Experience Bottle"),
    END_STONE_SHULKER("END_STONE_SHULKER".toInternalName(), "§9End Stone Shulker"),
    ENDSTONE_GEODE("ENDSTONE_GEODE".toInternalName(), "§9End Stone Geode"),
    MAGIC_RUNE("MAGIC_RUNE;1".toInternalName(), "§d◆ Magical Rune I"),
    MITE_GEL("MITE_GEL".toInternalName(), "§5Mite Gel"),
    SHRIMP_THE_FISH("SHRIMP_THE_FISH".toInternalName(), "§cShrimp the Fish"),

    END_HELMET("END_HELMET".toInternalName(), "§5Ender Helmet", NodeDropType.ARMOR),
    END_CHESTPLATE("END_CHESTPLATE".toInternalName(), "§5Ender Chestplate", NodeDropType.ARMOR),
    END_LEGGINGS("END_LEGGINGS".toInternalName(), "§5Ender Leggings", NodeDropType.ARMOR),
    END_BOOTS("END_BOOTS".toInternalName(), "§5Ender Boots", NodeDropType.ARMOR),
    ENDER_NECKLACE("ENDER_NECKLACE".toInternalName(), "§5Ender Necklace", NodeDropType.ARMOR),
    ENDER_GAUNTLET("ENDER_GAUNTLET".toInternalName(), "§5Ender Gauntlet", NodeDropType.ARMOR),

    COMMON_ENDERMAN_PET("ENDERMAN;0".toInternalName(), "§fEnderman", NodeDropType.PET),
    UNCOMMON_ENDERMAN_PET("ENDERMAN;1".toInternalName(), "§aEnderman", NodeDropType.PET),
    RARE_ENDERMAN_PET("ENDERMAN;2".toInternalName(), "§9Enderman", NodeDropType.PET),
    EPIC_ENDERMAN_PET("ENDERMAN;3".toInternalName(), "§5Enderman", NodeDropType.PET),
    LEGENDARY_ENDERMAN_PET("ENDERMAN;4".toInternalName(), "§6Enderman", NodeDropType.PET)
    ;

    private enum class NodeDropType {
        MISC,
        ARMOR,
        PET,
    }

    companion object {
        val miscEntries = entries.filter { it.dropType == NodeDropType.MISC }
        val armorEntries = entries.filter { it.dropType == NodeDropType.ARMOR }
        val petEntries = entries.filter { it.dropType == NodeDropType.PET }
    }

    // Todo: We really shouldn't have this - the enums should be in sync enough that we can 1:1 go name to name
    //  Migrations to actually do this would be hell.
    fun toEnderNodeDisplayEntryOrNull(): EnderNodeConfig.EnderNodeDisplayEntry? = when (this) {
        ENCHANTED_ENDSTONE -> EnderNodeConfig.EnderNodeDisplayEntry.ENCHANTED_END_STONE
        ENCHANTED_OBSIDIAN -> EnderNodeConfig.EnderNodeDisplayEntry.ENCHANTED_OBSIDIAN
        ENCHANTED_ENDER_PEARL -> EnderNodeConfig.EnderNodeDisplayEntry.ENCHANTED_ENDER_PEARL
        GRAND_EXP_BOTTLE -> EnderNodeConfig.EnderNodeDisplayEntry.GRAND_XP_BOTTLE
        TITANIC_EXP_BOTTLE -> EnderNodeConfig.EnderNodeDisplayEntry.TITANIC_XP_BOTTLE
        END_STONE_SHULKER -> EnderNodeConfig.EnderNodeDisplayEntry.END_STONE_SHULKER
        ENDSTONE_GEODE -> EnderNodeConfig.EnderNodeDisplayEntry.END_STONE_GEODE
        MAGIC_RUNE -> EnderNodeConfig.EnderNodeDisplayEntry.MAGICAL_RUNE_I
        MITE_GEL -> EnderNodeConfig.EnderNodeDisplayEntry.MITE_GEL
        SHRIMP_THE_FISH -> EnderNodeConfig.EnderNodeDisplayEntry.SHRIMP_THE_FISH
        END_HELMET -> EnderNodeConfig.EnderNodeDisplayEntry.ENDER_HELMET
        END_CHESTPLATE -> EnderNodeConfig.EnderNodeDisplayEntry.ENDER_CHESTPLATE
        END_LEGGINGS -> EnderNodeConfig.EnderNodeDisplayEntry.ENDER_LEGGINGS
        END_BOOTS -> EnderNodeConfig.EnderNodeDisplayEntry.ENDER_BOOTS
        ENDER_NECKLACE -> EnderNodeConfig.EnderNodeDisplayEntry.ENDER_NECKLACE
        ENDER_GAUNTLET -> EnderNodeConfig.EnderNodeDisplayEntry.ENDER_GAUNTLET
        COMMON_ENDERMAN_PET -> EnderNodeConfig.EnderNodeDisplayEntry.ENDERMAN_PET
        UNCOMMON_ENDERMAN_PET -> EnderNodeConfig.EnderNodeDisplayEntry.ENDERMAN_PET
        else -> null
    }
}
