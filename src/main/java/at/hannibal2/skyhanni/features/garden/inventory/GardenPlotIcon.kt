package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import io.github.moulberry.notenoughupdates.events.ReplaceItemEvent
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object GardenPlotIcon {

    private val config get() = SkyHanniMod.feature.garden.plotIcon
    private val plotList get() = GardenAPI.storage?.plotIcon?.plotList
    private var inInventory = false
    private var copyStack: ItemStack? = null
    private var editMode = 0 // 0 = off, 1 = on, 2 = reset
    private var lastClickedSlotId = -1
    private var originalStack = mutableMapOf<Int, ItemStack>()
    private var cachedStack = mutableMapOf<Int, ItemStack>()
    private val editStack = ItemStack(Items.wooden_axe)
    private val whitelistedSlot =
        listOf(2, 3, 4, 5, 6, 11, 12, 13, 14, 15, 20, 21, 23, 24, 29, 30, 31, 32, 33, 38, 39, 40, 41, 42)

    var hardReset = false

    fun isEnabled() = GardenAPI.inGarden() && config.enabled && inInventory

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        inInventory = event.inventoryName == "Configure Plots"
        if (!isEnabled()) return

        for ((index, stack) in event.inventoryItems) {
            originalStack[index] = stack
        }
        val plotList = plotList ?: return
        for ((index, internalName) in plotList) {
            val old = originalStack[index]!!
            val new = internalName.getItemStack()
            cachedStack[index] = Utils.editItemStackInfo(new, old.displayName, true, *old.getLore().toTypedArray())
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
        editMode = 0
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun replaceItem(event: ReplaceItemEvent) {
        if (!isEnabled()) return
        val plotList = plotList ?: return
        if (hardReset) {
            plotList.clear()
            hardReset = false
            return
        }

        if (event.inventory is ContainerLocalMenu) {
            if (event.slotNumber == 53) {
                event.replaceWith(editStack)
            }
            if (plotList.isNotEmpty() && plotList.contains(event.slotNumber)) {
                if (lastClickedSlotId == event.slotNumber) {
                    lastClickedSlotId = -1
                    return
                }
                event.replaceWith(cachedStack[event.slotNumber])
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onStackClick(event: SlotClickEvent) {
        if (!isEnabled()) return
        lastClickedSlotId = event.slotId
        if (event.slotId == 53) {
            event.isCanceled = true
            if (event.clickedButton == 0) {
                if (editMode == 2)
                    editMode = 0
                else
                    editMode++
            } else if (event.clickedButton == 1) {
                if (editMode == 0)
                    editMode = 2
                else
                    editMode--
            }
            return
        }
        if (editMode != 0) {
            if (event.slotId in 54..89) {
                event.isCanceled = true
                copyStack = event.slot.stack ?: return
                // TODO different format, not bold or show not in chat at all.
                LorenzUtils.chat("§6§lClick an item in the desk menu to replace it with that item!")
                return
            }
            if (event.slotId != 53) {
                val plotList = plotList ?: return
                if (!whitelistedSlot.contains(event.slotId)) return
                event.isCanceled = true
                if (editMode == 2) {
                    plotList.remove(event.slotId)
                    return
                }
                val copyStack = copyStack ?: return
                plotList[event.slotId] = copyStack.getInternalName()
                cachedStack[event.slotId] = copyStack
            }
        }
    }

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!isEnabled()) return
        val plotList = plotList ?: return
        val list = event.toolTip
        val index = event.slot.slotNumber
        if (index == 53) {
            list.clear()
            list.add("§6Edit Mode")
            list.add("")
            list.add("${if (editMode == 0) "§6► " else ""}§cOFF§7: §bNothing change, behave like normal.")
            list.add("${if (editMode == 1) "§6► " else ""}§aON§7: §bClick an item in your inventory then click again")
            list.add("${if (editMode == 1) "§6► " else ""}§bin the plot menu to change it to that item.")
            list.add("${if (editMode == 2) "§6► " else ""}§9RESET§7: §bClick an item in the menu to reset it to default.")
            list.add("")
            list.add("§eClick to switch Edit Mode !")
            list.add("")
        }
        if (plotList.contains(index)) {
            val stack = originalStack[index] ?: return
            val lore = stack.getLore()
            list.clear()
            list.add(0, stack.displayName)
            for (i in lore.indices) {
                list.add(i + 1, stack.getLore()[i])
            }
        }
    }
}
