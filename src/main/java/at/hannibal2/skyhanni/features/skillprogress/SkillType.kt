package at.hannibal2.skyhanni.features.skillprogress

import at.hannibal2.skyhanni.utils.ItemUtils
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.item.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

enum class SkillType(val displayName: String, icon: Item, val maxLevel: Int) {
    COMBAT("Combat", Items.GOLDEN_SWORD, 60),
    FARMING("Farming", Items.GOLDEN_HOE, 60),
    FISHING("Fishing", Items.FISHING_ROD, 50),
    MINING("Mining", Items.GOLDEN_PICKAXE, 60),
    FORAGING("Foraging", Items.GOLDEN_AXE, 54),
    ENCHANTING("Enchanting", Blocks.ENCHANTING_TABLE, 60),
    ALCHEMY("Alchemy", Items.BREWING_STAND, 50),
    CARPENTRY("Carpentry", Blocks.CRAFTING_TABLE, 50),
    //#if MC < 1.16
    //$$ TAMING("Taming", Items.spawn_egg, 60),
    //#else
    TAMING("Taming", Items.POLAR_BEAR_SPAWN_EGG, 60),
    //#endif
    HUNTING("Hunting", Items.LEAD, 25),
    ;

    constructor(displayName: String, block: Block, maxLevel: Int) : this(displayName, Item.fromBlock(block), maxLevel)

    val item: ItemStack by lazy { ItemUtils.createItemStack(icon, displayName) }
    val lowercaseName = displayName.lowercase()
    val uppercaseName = displayName.uppercase()

    override fun toString(): String = "§b$displayName"

    companion object {
        fun getByName(name: String) = getByNameOrNull(name) ?: error("Unknown Skill Type: '$name'")

        fun getByNameOrNull(name: String) =
            entries.firstOrNull { it.displayName.lowercase() == name.lowercase() }
    }
}
