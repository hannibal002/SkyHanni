package at.hannibal2.skyhanni.features.garden.inventory


import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils.chat
import io.github.moulberry.notenoughupdates.events.ReplaceItemEvent
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenPlotIcon {

    private val config get() = SkyHanniMod.feature.garden.plotIcon
    private var showItem = false
    private var copyStack: ItemStack? = null
    private var editMode = 0 // 0 = off, 1 = on, 2 = reset
    private var lastClickedSlotId = -1
    private var originalStack = mutableMapOf<Int, ItemStack>()

    companion object {
        var hardReset = false
        private val plotList get() = GardenAPI.config?.plotIcon?.plotList
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        for ((index, stack) in event.inventoryItems){
            originalStack[index] = stack
        }
        showItem = GardenAPI.inGarden() && event.inventoryName == "Configure Plots"
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        showItem = false
        editMode = 0
        copyStack = null
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun replaceItem(event: ReplaceItemEvent) {
        if (!config.enabled) return
        if (!showItem) return
        val plotList = GardenPlotIcon.plotList ?: return

        if (hardReset) {
            plotList.clear()
            hardReset = false
            return
        }

        if (event.inventory is ContainerLocalMenu) {
            if (event.slotNumber == 53) {
                val mode: String = when (editMode) {
                    0 -> "§cOFF"
                    1 -> "§aON"
                    2 -> "§9RESET"
                    else -> ""
                }
                event.replaceWith(Utils.createItemStack(ItemStack(Items.wooden_axe).item, "§6Edit Mode [$mode§6]", "", "§bSwitch Edit Mode"))
            }

            if (plotList.isNotEmpty() && plotList.contains(event.slotNumber)) {
                val stack = originalStack[event.slotNumber]
                val name = stack?.displayName ?: "§cError getting item name"
                val lore = stack?.getLore()?.toTypedArray() ?: arrayOf("§cError getting item lore")
                if (lastClickedSlotId == event.slotNumber) {
                    lastClickedSlotId = -1
                    return
                }
                val replaceStack = Utils.editItemStackInfo(event.slotNumber.getItem(), name, true, *lore)
                event.replaceWith(replaceStack)
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onStackClick(event: SlotClickEvent) {
        if (!config.enabled) return
        val plotList = GardenPlotIcon.plotList ?: return
        lastClickedSlotId = event.slotId
        if (showItem) {
            if (event.slotId == 53) {
                event.isCanceled = true
                if (editMode == 2)
                    editMode = 0
                else
                    editMode++
                return
            }
            if (editMode != 0) {
                if (event.slotId in 54..89) {
                    event.isCanceled = true
                    if (event.slot.stack == null) return
                    copyStack = event.slot.stack
                    chat("§6§lClick an item in the desk menu to replace it with that item!")
                    return
                }
                if (event.slotId != 53) {
                    event.isCanceled = true
                    if (editMode == 2) {
                        plotList.remove(event.slotId)
                        return
                    }
                    event.slotId.setItem(copyStack)
                    return
                }
            }
        }
    }

    private fun getFallbackItem() =
            ItemStack(Blocks.barrier).setStackDisplayName("§cError loading item")

    private fun Int.setItem(stack: ItemStack?) {
        val gardenPlot = GardenAPI.config?.plotIcon ?: return
        val plotList = gardenPlot.plotList

        plotList[this] = stack
        return
    }

    private fun Int.getItem(): ItemStack? {
        val gardenPlot = GardenAPI.config?.plotIcon ?: return null

        val plotList = gardenPlot.plotList
        plotList[this]?.let { return it }

        plotList[this] = null
        return null
    }

}