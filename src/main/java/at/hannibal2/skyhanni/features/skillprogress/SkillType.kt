package at.hannibal2.skyhanni.features.skillprogress

import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

enum class SkillType(val displayName: String, icon: Item) {
    ALCHEMY("Alchemy", Items.brewing_stand),
    CARPENTRY("Carpentry", Blocks.crafting_table),
    COMBAT("Combat", Items.golden_sword),
    ENCHANTING("Enchanting", Blocks.enchanting_table),
    FARMING("Farming", Items.golden_hoe),
    FISHING("Fishing", Items.fishing_rod),
    FORAGING("Foraging", Items.golden_axe),
    MINING("Mining", Items.golden_pickaxe),
    TAMING("Taming", Items.spawn_egg),
    ;

    constructor(displayName: String, block: Block) : this(displayName, Item.getItemFromBlock(block))

    val item: ItemStack by lazy { Utils.createItemStack(icon, displayName) }
    val lowercaseName = displayName.lowercase()
    val uppercaseName = displayName.uppercase()

    override fun toString(): String = "§b$displayName"

    companion object {
        fun getByName(name: String) = getByNameOrNull(name) ?: error("Unknown Skill Type: '$name'")

        fun getByNameOrNull(name: String) =
            entries.firstOrNull { it.displayName.lowercase() == name.lowercase() }
    }
}
