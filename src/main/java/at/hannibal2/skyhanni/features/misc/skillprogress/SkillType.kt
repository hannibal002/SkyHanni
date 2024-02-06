package at.hannibal2.skyhanni.features.misc.skillprogress

import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

enum class SkillType(val displayName: String,
                     itemSupplier: () -> ItemStack
) {
    FARMING("Farming", { Utils.createItemStack(Items.golden_hoe, "Farming") }),
    COMBAT("Combat", { Utils.createItemStack(Items.golden_sword, "Combat") }),
    FORAGING("Foraging", { Utils.createItemStack(Items.golden_axe, "Foraging") }),
    ALCHEMY("Alchemy", { Utils.createItemStack(Items.brewing_stand, "Alchemy") }),
    MINING("Mining", { Utils.createItemStack(Items.golden_pickaxe, "Mining") }),
    ENCHANTING("Enchanting", { Utils.createItemStack(Blocks.enchanting_table, "Enchanting") }),
    FISHING("Fishing", { Utils.createItemStack(Items.fishing_rod, "Fishing") }),
    CARPENTRY("Carpentry", { Utils.createItemStack(Blocks.crafting_table, "Carpentry") }),
    ;

    val item by lazy { itemSupplier() }

    companion object {
        fun getByName(name: String) = getByNameOrNull(name) ?: error("Unknown Skill Type: '$name'")
        fun getByNameLowercase(name: String) = entries.firstOrNull { it.displayName.lowercase() == name } ?: error("Unknown Skill Type: '$name'")
        fun getByNameUppercase(name: String) = entries.firstOrNull { it.displayName.uppercase() == name } ?: error("Unknown Skill Type: '$name'")
        private fun getByNameOrNull(name: String) = entries.firstOrNull { it.displayName == name }
    }
}
