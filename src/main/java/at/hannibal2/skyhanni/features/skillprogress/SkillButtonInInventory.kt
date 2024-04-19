package at.hannibal2.skyhanni.features.skillprogress

import at.hannibal2.skyhanni.api.SkillAPI.customGoalConfig
import at.hannibal2.skyhanni.api.SkillAPI.overflowConfig
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.utils.ItemUtils.createToggleItem
import io.github.moulberry.notenoughupdates.events.ReplaceItemEvent
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SkillButtonInInventory {

    private var showItem = false

    private val toggleStackSizeEnabled by lazy {
        createToggleItem(true, "Stack Size")
    }

    private val toggleTooltipEnabled by lazy {
        createToggleItem(true, "Tooltip")
    }

    private val toggleCustomGoalEnabled by lazy {
        createToggleItem(true, "Custom Goal")
    }

    private val toggleStackSizeDisabled by lazy {
        createToggleItem(false, "Stack Size")
    }

    private val toggleTooltipDisabled by lazy {
        createToggleItem(false, "Tooltip")
    }

    private val toggleCustomGoalDisabled by lazy {
        createToggleItem(false, "Custom Goal")
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        showItem = event.inventoryName == "Your Skills"
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        showItem = false
    }

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (event.inventory is ContainerLocalMenu && showItem) {
            if (event.slotNumber == 50 && overflowConfig.tooltipButtonInSkillMenu) {
                val itemToShow = if (overflowConfig.enableInSkillMenuTooltip) toggleTooltipEnabled else toggleTooltipDisabled
                event.replaceWith(itemToShow)
            }
            if (event.slotNumber == 51 && overflowConfig.stackSizeButtonInSkillMenu) {
                val itemToShow = if (overflowConfig.enableInSkillMenuAsStackSize) toggleStackSizeEnabled else toggleStackSizeDisabled
                event.replaceWith(itemToShow)
            }
            if (event.slotNumber == 52 && customGoalConfig.customGoalButtonInSkillMenu) {
                val itemToShow = if (customGoalConfig.enableInSkillMenuTooltip) toggleCustomGoalEnabled else toggleCustomGoalDisabled
                event.replaceWith(itemToShow)
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onStackClick(event: GuiContainerEvent.SlotClickEvent) {
        if (showItem) {
            if (event.slotId == 50 && overflowConfig.tooltipButtonInSkillMenu) {
                event.isCanceled = true
                overflowConfig.enableInSkillMenuTooltip = !overflowConfig.enableInSkillMenuTooltip
            }
            if (event.slotId == 51 && overflowConfig.stackSizeButtonInSkillMenu) {
                event.isCanceled = true
                overflowConfig.enableInSkillMenuAsStackSize = !overflowConfig.enableInSkillMenuAsStackSize
            }
            if (event.slotId == 52 && customGoalConfig.customGoalButtonInSkillMenu) {
                event.isCanceled = true
                customGoalConfig.enableInSkillMenuTooltip = !customGoalConfig.enableInSkillMenuTooltip
            }
        }
    }
}
