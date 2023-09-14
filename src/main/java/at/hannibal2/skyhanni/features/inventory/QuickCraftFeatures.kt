package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
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

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled() || !quickCraftSlots.contains(event.slot?.slotNumber)) return

        val clickedItem = event.slot?.stack ?: return

        if (!LorenzUtils.isControlKeyDown() && needsQuickCraftConfirmation(clickedItem)) {
            event.isCanceled = true
        }
    }

    private fun needsQuickCraftConfirmation(item: ItemStack): Boolean {
        return !quickCraftableItems.contains(item.displayName.removeColor())
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enableQuickCraftingConfirmation && InventoryUtils.openInventoryName() == "Craft Item"

}