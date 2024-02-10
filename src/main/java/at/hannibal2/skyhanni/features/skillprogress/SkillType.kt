package at.hannibal2.skyhanni.features.skillprogress

import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

enum class SkillType(val displayName: String,
                     itemSupplier: () -> ItemStack
) {
    ALCHEMY("Alchemy", { Utils.createItemStack(Items.brewing_stand, "Alchemy") }),
    CARPENTRY("Carpentry", { Utils.createItemStack(Blocks.crafting_table, "Carpentry") }),
    COMBAT("Combat", { Utils.createItemStack(Items.golden_sword, "Combat") }),
    ENCHANTING("Enchanting", { Utils.createItemStack(Blocks.enchanting_table, "Enchanting") }),
    FARMING("Farming", { Utils.createItemStack(Items.golden_hoe, "Farming") }),
    FISHING("Fishing", { Utils.createItemStack(Items.fishing_rod, "Fishing") }),
    FORAGING("Foraging", { Utils.createItemStack(Items.golden_axe, "Foraging") }),
    MINING("Mining", { Utils.createItemStack(Items.golden_pickaxe, "Mining") }),
    TAMING("Taming", { Utils.createItemStack(Items.spawn_egg, "Taming") }),

    NONE("", { ItemStack(Blocks.bedrock) })
    ;

    val item by lazy { itemSupplier() }
    val lowercaseName = displayName.lowercase()
    val uppercaseName = displayName.uppercase()

    companion object {
        fun getByName(name: String) = getByNameOrNull(name) ?: error("Unknown Skill Type: '$name'")
        fun getByNameLowercase(name: String) = entries.firstOrNull { it.displayName.lowercase() == name }
        fun getByNameUppercase(name: String) = entries.firstOrNull { it.displayName.uppercase() == name }
        fun getByNameFirstUppercase(name: String) =
            entries.firstOrNull { it.displayName.firstLetterUppercase() == name }

        private fun getByNameOrNull(name: String) = entries.firstOrNull { it.displayName == name }
    }
}
