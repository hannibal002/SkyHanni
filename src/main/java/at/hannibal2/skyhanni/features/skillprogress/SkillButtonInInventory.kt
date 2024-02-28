package at.hannibal2.skyhanni.features.skillprogress

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import io.github.moulberry.notenoughupdates.events.ReplaceItemEvent
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SkillButtonInInventory {

    private var showItem = false

    private val toggleStackSizeEnabled by lazy {
        val stack = ItemStack(Blocks.stained_hardened_clay)
        Utils.createItemStack(
            stack.item,
            "§aStack Size Currently ON",
            13,
            "",
            "§7§eClick to toggle!")
    }

    private val toggleTooltipEnabled by lazy {
        val stack = ItemStack(Blocks.stained_hardened_clay)
        Utils.createItemStack(
            stack.item,
            "§aTooltip Currently ON",
            13,
            "",
            "§7§eClick to toggle!")

    }

    private val toggleCustomGoalEnabled by lazy {
        val stack = ItemStack(Blocks.stained_hardened_clay)
        Utils.createItemStack(
            stack.item,
            "§aCustom Goal Currently ON",
            13,
            "",
            "§7§eClick to toggle!")

    }

    private val toggleStackSizeDisabled by lazy {
        val stack = ItemStack(Blocks.stained_hardened_clay)
        Utils.createItemStack(
            stack.item,
            "§cStack Size Currently OFF",
            14,
            "",
            "§7§eClick to toggle!")
    }

    private val toggleTooltipDisabled by lazy {
        val stack = ItemStack(Blocks.stained_hardened_clay)
        Utils.createItemStack(
            stack.item,
            "§cTooltip Currently OFF",
            14,
            "",
            "§7§eClick to toggle!")
    }

    private val toggleCustomGoalDisabled by lazy {
        val stack = ItemStack(Blocks.stained_hardened_clay)
        Utils.createItemStack(
            stack.item,
            "§cCustom Goal Currently OFF",
            14,
            "",
            "§7§eClick to toggle!")
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
            if (event.slotNumber == 50 && SkyHanniMod.feature.skillProgress.overflowConfig.tooltipButtonInSkillMenu) {
                val itemToShow = if (SkyHanniMod.feature.skillProgress.overflowConfig.enableInSkillMenuTooltip) toggleTooltipEnabled else toggleTooltipDisabled
                event.replaceWith(itemToShow)
            }
            if (event.slotNumber == 51 && SkyHanniMod.feature.skillProgress.overflowConfig.stackSizeButtonInSkillMenu) {
                val itemToShow = if (SkyHanniMod.feature.skillProgress.overflowConfig.enableInSkillMenuAsStackSize) toggleStackSizeEnabled else toggleStackSizeDisabled
                event.replaceWith(itemToShow)
            }
            if (event.slotNumber == 52 && SkyHanniMod.feature.skillProgress.customGoalConfig.customGoalButtonInSkillMenu){
                val itemToShow = if (SkyHanniMod.feature.skillProgress.customGoalConfig.enableInSkillMenuTooltip) toggleCustomGoalEnabled else toggleCustomGoalDisabled
                event.replaceWith(itemToShow)
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onStackClick(event: SlotClickEvent) {
        if (showItem) {
            if (event.slotId == 50 && SkyHanniMod.feature.skillProgress.overflowConfig.tooltipButtonInSkillMenu) {
                event.isCanceled = true
                SkyHanniMod.feature.skillProgress.overflowConfig.enableInSkillMenuTooltip = !SkyHanniMod.feature.skillProgress.overflowConfig.enableInSkillMenuTooltip
            }
            if (event.slotId == 51 && SkyHanniMod.feature.skillProgress.overflowConfig.stackSizeButtonInSkillMenu) {
                event.isCanceled = true
                SkyHanniMod.feature.skillProgress.overflowConfig.enableInSkillMenuAsStackSize = !SkyHanniMod.feature.skillProgress.overflowConfig.enableInSkillMenuAsStackSize
            }
            if (event.slotId == 52 && SkyHanniMod.feature.skillProgress.customGoalConfig.customGoalButtonInSkillMenu) {
                event.isCanceled = true
                SkyHanniMod.feature.skillProgress.customGoalConfig.enableInSkillMenuTooltip = !SkyHanniMod.feature.skillProgress.customGoalConfig.enableInSkillMenuTooltip
            }
        }
    }
}
