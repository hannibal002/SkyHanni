package at.hannibal2.skyhanni.features.misc.skillprogress

import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

enum class SkillType(val item: ItemStack) {
    FARMING(Utils.createItemStack(Items.golden_hoe, "Farming")),
    COMBAT(Utils.createItemStack(Items.golden_sword, "Combat")),
    FORAGING(Utils.createItemStack(Items.golden_axe, "Foraging")),
    ALCHEMY(Utils.createItemStack(Items.brewing_stand, "Alchemy")),
    MINING(Utils.createItemStack(Items.golden_pickaxe, "Mining")),
    ENCHANTING(Utils.createItemStack(Blocks.enchanting_table, "Enchanting")),
    FISHING(Utils.createItemStack(Items.fishing_rod, "Fishing")),
    CARPENTRY(Utils.createItemStack(Blocks.crafting_table, "Carpentry")),
    ;
}
