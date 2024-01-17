@file:Suppress("SENSELESS_COMPARISON") // intellij does a little trolling and assumes all ItemStacks will never be null. this suppresses such warnings.
package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.command.CopyItemCommand.grabItemData
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object TestCopyChestData {
    private val config get() = SkyHanniMod.feature.dev.debug
    private val mc = Minecraft.getMinecraft()
    @SubscribeEvent
    fun onKeybind(event: GuiScreenEvent.KeyboardInputEvent.Post) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.copyPlayerInventory.isKeyHeld() && !config.copyEntireChest.isKeyHeld() && !config.copyChestName.isKeyHeld()) return
        if (event.gui !is GuiContainer) return
        if (config.copyEntireChest.isKeyHeld()) {
            copySlotsData(chest = (event.gui as GuiChest).inventorySlots.inventorySlots)
            return
        } else if (config.copyPlayerInventory.isKeyHeld()) {
            copySlotsData(inventory = if (config.includeArmor) mc.thePlayer.inventory.mainInventory + mc.thePlayer.inventory.armorInventory else mc.thePlayer.inventory.mainInventory)
            return
        } else if (InventoryUtils.openInventoryName().isNotEmpty()) {
            OSUtils.copyToClipboard(InventoryUtils.openInventoryName())
            LorenzUtils.chat("Chest name copied to clipboard.")
            return
        }
    }
    private fun copySlotsData(chest: List<Slot> = emptyList(), inventory: Array<ItemStack> = emptyArray()) {
        val copyList = mutableListOf<String>("relevant config:", "includeNullSlots: ${config.includeNullSlots}", "includeUnnamedItems: ${config.includeUnnamedItems}", "includeArmor: ${config.includeArmor}", "")
        val inventoryType: String = if (chest.isNotEmpty()) "Chest" else if (inventory.isNotEmpty()) "Inventory" else "Unknown"
        if (chest.isNotEmpty()) {
            copyList.addAll(listOf<String>("chest name: '${InventoryUtils.openInventoryName()}'", ""))
            for (slot in chest) {
                val stack = slot.stack
                if (stack == null) {
                    if (config.includeNullSlots) copyList.addAll(listOf("(there is nothing inside slot ${slot.slotIndex}; it is null)", "", ""))
                    continue
                }
                if (stack in mc.thePlayer.inventory.mainInventory) break
                if ((stack.displayName.isNotEmpty() && stack.displayName.isNotBlank()) || config.includeUnnamedItems) {
                    copyList.add("slot index: '${slot.slotIndex}'")
                    copyList.addAll(stack.getStackInfo())
                }
            }
        } else if (inventory.isNotEmpty()) {
            copyList.addAll(listOf<String>("your inventory is below.", "", "your hotbar:", ""))
            for ((i, stack) in inventory.withIndex()) {
                if (i - 1 == 8 && stack != null) copyList.addAll(listOf("note: hotbar ends here", "the rest of your inventory:", "", ""))
                if (i - 1 == 35 && config.includeArmor && stack != null) copyList.addAll(listOf("note: the rest of your inventory data ends here", "your armor:", "", ""))
                if (stack == null) {
                    if (config.includeNullSlots) copyList.addAll(listOf("(there is nothing inside slot $i; it is null)", "", ""))
                    continue
                }
                if ((stack.displayName.isNotEmpty() && stack.displayName.isNotBlank()) || config.includeUnnamedItems) {
                    copyList.add("slot index: '$i'")
                    copyList.addAll(stack.getStackInfo())
                }
            }
        }
        OSUtils.copyToClipboard(copyList.joinToString("\n"))
        LorenzUtils.chat("$inventoryType data copied into the clipboard! §lMake sure to save it into a .txt file; these tend to get quite long.")
    }
    private fun ItemStack.getStackInfo(): List<String> {
        val returnList = mutableListOf<String>()
        returnList.addAll(listOf<String>("stack size: '${this.stackSize}'", "is stackable: '${this.isStackable}'", "item data (begins on next line):"))
        returnList.addAll(grabItemData(this))
        returnList.addAll(listOf<String>("", ""))
        return returnList
    }
}
