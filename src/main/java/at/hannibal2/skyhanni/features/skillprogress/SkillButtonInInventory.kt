package at.hannibal2.skyhanni.features.skillprogress

import at.hannibal2.skyhanni.api.SkillAPI.customGoalConfig
import at.hannibal2.skyhanni.api.SkillAPI.overflowConfig
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.createToggleItem
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object SkillButtonInInventory {

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
            if (event.slot == 50 && overflowConfig.tooltipButtonInSkillMenu) {
                val itemToShow = if (overflowConfig.enableInSkillMenuTooltip) toggleTooltipEnabled else toggleTooltipDisabled
                event.replace(itemToShow)
            }
            if (event.slot == 51 && overflowConfig.stackSizeButtonInSkillMenu) {
                val itemToShow = if (overflowConfig.enableInSkillMenuAsStackSize) toggleStackSizeEnabled else toggleStackSizeDisabled
                event.replace(itemToShow)
            }
            if (event.slot == 52 && customGoalConfig.customGoalButtonInSkillMenu) {
                val itemToShow = if (customGoalConfig.enableInSkillMenuTooltip) toggleCustomGoalEnabled else toggleCustomGoalDisabled
                event.replace(itemToShow)
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
