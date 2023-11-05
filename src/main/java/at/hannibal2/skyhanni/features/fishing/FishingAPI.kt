package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack

object FishingAPI {
    fun hasFishingRodInHand() = InventoryUtils.itemInHandId.asString().contains("ROD")

    fun ItemStack.isBait(): Boolean {
        val name = name ?: return false
        return stackSize == 1 && (name.removeColor().startsWith("Obfuscated") || name.endsWith(" Bait"))
    }
}
