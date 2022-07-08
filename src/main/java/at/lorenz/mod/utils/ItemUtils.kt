package at.lorenz.mod.utils

import at.lorenz.mod.utils.LorenzUtils.matchRegex
import at.lorenz.mod.utils.LorenzUtils.removeColorCodes
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.item.ItemStack

object ItemUtils {

    fun ItemStack.cleanName() = this.displayName.removeColorCodes()

    fun getItemsInOpenChest(): List<ItemStack> {
        val list = mutableListOf<ItemStack>()
        val guiChest = Minecraft.getMinecraft().currentScreen as GuiChest
        val inventorySlots = guiChest.inventorySlots.inventorySlots
        val skipAt = inventorySlots.size - 9 * 4
        var i = 0
        for (slot in inventorySlots) {
            val stack = slot.stack
            if (stack != null) {
                list.add(stack)
            }
            i++
            if (i == skipAt) break
        }
        return list
    }

    fun isSack(name: String): Boolean = name.endsWith(" Sack")

    fun ItemStack.getLore() = ItemUtil.getItemLore(this)

    fun isCoOpSoulBound(stack: ItemStack): Boolean = stack.getLore().any { it.contains("Co-op Soulbound") }

    fun isRecombobulated(stack: ItemStack): Boolean = stack.getLore().any { it.contains("§k") }

    fun isPet(name: String): Boolean = name.matchRegex("\\[Lvl (.*)] (.*)") && !listOf(
        "Archer",
        "Berserk",
        "Mage",
        "Tank",
        "Healer",
        "➡",
    ).any { name.contains(it) }

    fun maxPetLevel(name: String) = if (name.contains("Golden Dragon")) 200 else 100

}