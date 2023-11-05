package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack

object FishingAPI {
    private val lavaBlocks = listOf(Blocks.lava, Blocks.flowing_lava)
    private val waterBlocks = listOf(Blocks.water, Blocks.flowing_water)

    fun hasFishingRodInHand() = InventoryUtils.itemInHandId.asString().contains("ROD")

    fun ItemStack.isBait(): Boolean {
        val name = name ?: return false
        return stackSize == 1 && (name.removeColor().startsWith("Obfuscated") || name.endsWith(" Bait"))
    }

    fun isLavaRod() = InventoryUtils.getItemInHand()?.getLore()?.any { it.contains("Lava Rod") } ?: false

    fun getAllowedBlocks() = if (isLavaRod()) lavaBlocks else waterBlocks
}
