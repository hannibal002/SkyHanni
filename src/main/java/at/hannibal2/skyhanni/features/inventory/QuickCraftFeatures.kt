package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ItemRenderBackground.Companion.background
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class QuickCraftFeatures {
    private val config get() = SkyHanniMod.feature.inventory
    private val quickCraftSlots = listOf(16, 25, 34)
    private var quickCraftableItems = emptyList<String>()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        quickCraftableItems = event.getConstant<List<String>>("QuickCraftableItems") ?: emptyList()
    }

    @SubscribeEvent
    fun onToolTip(event: LorenzToolTipEvent) {
        if (!isEnabled() || !quickCraftSlots.contains(event.slot.slotNumber)) return

        if (needsQuickCraftConfirmation(event.itemStack)) {
            event.toolTip.replaceAll { it.replace("Click to craft!", "§cCtrl+Click to craft!") }
        }
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        if (KeyboardManager.isControlKeyDown()) return
        if (event.gui !is GuiChest) return
        val chest = event.gui.inventorySlots as ContainerChest

        for (slot in chest.inventorySlots) {
            if (slot == null) continue
            if (slot.slotNumber !in quickCraftSlots) continue
            val stack = slot.stack ?: continue
            val name = stack.name ?: continue
            if (name == "§cQuick Crafting Slot") continue
            if (needsQuickCraftConfirmation(stack)) {
                val color = LorenzColor.DARK_GRAY.addOpacity(180)
                stack.background = color.rgb
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled() || !quickCraftSlots.contains(event.slot?.slotNumber)) return

        val clickedItem = event.slot?.stack ?: return

        if (!KeyboardManager.isControlKeyDown() && needsQuickCraftConfirmation(clickedItem)) {
            event.isCanceled = true
        }
    }

    private fun needsQuickCraftConfirmation(item: ItemStack): Boolean {
        return !quickCraftableItems.contains(item.displayName.removeColor())
    }

    fun isEnabled() =
        LorenzUtils.inSkyBlock && config.quickCraftingConfirmation && InventoryUtils.openInventoryName() == "Craft Item"

}