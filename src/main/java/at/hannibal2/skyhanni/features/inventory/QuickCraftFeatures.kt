package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.getAllItems
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.item.ItemStack

@SkyHanniModule
object QuickCraftFeatures {

    private val config get() = SkyHanniMod.feature.inventory
    private val quickCraftSlots = listOf(16, 25, 34)
    private var quickCraftableItems = emptyList<String>()

    enum class InventoryType(val inventoryName: String) {
        CRAFT_ITEM("Craft Item"),
        MORE_QUICK_CRAFT_OPTIONS("Quick Crafting"),
    }

    private fun InventoryType.ignoreSlot(slotNumber: Int?): Boolean = when (this) {
        InventoryType.CRAFT_ITEM -> slotNumber !in quickCraftSlots
        InventoryType.MORE_QUICK_CRAFT_OPTIONS -> slotNumber !in 10..44
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        quickCraftableItems = event.getConstant<List<String>>("QuickCraftableItems")
    }

    @HandleEvent
    fun onToolTip(event: ToolTipTextEvent) {
        event.slot ?: return
        val inventoryType = getInventoryType() ?: return
        if (inventoryType.ignoreSlot(event.slot.index)) return

        if (needsQuickCraftConfirmation(event.itemStack)) {
            for ((index, line) in event.toolTip.withIndex()) {
                if (line.string.removeColor() == "Click to craft!") {
                    event.toolTip.set(index, Component.nullToEmpty("§c${KeyboardManager.getModifierKeyName()} + Click to craft!"))
                    break
                }
            }
        }
    }

    @HandleEvent
    fun onForegroundDrawn(event: GuiContainerEvent.ForegroundDrawnEvent) {
        val inventoryType = getInventoryType() ?: return
        if (KeyboardManager.isModifierKeyDown()) return
        if (event.gui !is ContainerScreen) return
        val chest = event.container as ChestMenu

        for ((slot, stack) in chest.getAllItems()) {
            if (inventoryType.ignoreSlot(slot.index)) continue
            if (stack.hoverName.formattedTextCompatLeadingWhiteLessResets() == "§cQuick Crafting Slot") continue
            if (needsQuickCraftConfirmation(stack)) {
                slot.highlight(LorenzColor.DARK_GRAY.addOpacity(180))
            }
        }
    }

    @HandleEvent(priority = HandleEvent.HIGH)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        val inventoryType = getInventoryType() ?: return
        if (inventoryType.ignoreSlot(event.slot?.index)) return

        val clickedItem = event.slot?.item ?: return
        if (!KeyboardManager.isModifierKeyDown() && needsQuickCraftConfirmation(clickedItem)) {
            event.cancel()
        }
    }

    private fun needsQuickCraftConfirmation(item: ItemStack): Boolean {
        return !quickCraftableItems.contains(item.hoverName.formattedTextCompatLeadingWhiteLessResets().removeColor())
    }

    private fun getInventoryType(): InventoryType? {
        if (!SkyBlockUtils.inSkyBlock || !config.quickCraftingConfirmation) return null

        val inventoryName = InventoryUtils.openInventoryName()
        return InventoryType.entries.firstOrNull { it.inventoryName == inventoryName }
    }
}
