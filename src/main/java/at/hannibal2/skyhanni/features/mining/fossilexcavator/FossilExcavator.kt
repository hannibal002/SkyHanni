package at.hannibal2.skyhanni.features.mining.fossilexcavator

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.SkyHanniMod.Companion.coroutineScope
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object FossilExcavator {

    private val config get() = SkyHanniMod.feature.mining.fossilExcavator

    private val patternGroup = RepoPattern.group("mining.fossilexcavator")
    private val chargesRemainingPattern by patternGroup.pattern(
        "chargesremaining",
        "Chisel Charges Remaining: (?<charges>\\d+)"
    )
    private val fossilProgressPattern by patternGroup.pattern(
        "fossilprogress",
        "Fossil Excavation Progress: (?<progress>[\\d.]+%)"
    )

    private var inInventory = false
    private var inExcavatorMenu = false

    private var foundPercentage = false
    private var percentage: String? = null
    private var chargesRemaining = 0
    private var possibleFossilsRemaining = 0

    private var slotToClick: Int? = null
    private var correctPercentage: String? = null

    private var isNotPossible = false
    private var isCompleted = false

    private var inventoryItemNames = listOf<String>()

    private const val NOT_POSSIBLE_STRING = "§cNo possible fossils on board."
    private const val SOLVED_STRING = "§aFossil found, get all the loot you can."
    private const val FOSSILS_REMAINING_STRING = "§ePossible fossils remaining: "

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (event.inventoryName != "Fossil Excavator") return
        inInventory = true
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        clearData()
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        clearData()
    }

    private fun clearData() {
        inInventory = false
        inExcavatorMenu = false
        foundPercentage = false
        percentage = null
        chargesRemaining = 0
        slotToClick = null
        correctPercentage = null
        isNotPossible = false
        isCompleted = false
        inventoryItemNames = emptyList()
    }

    @SubscribeEvent
    fun onInventoryUpdate(event: InventoryUpdatedEvent) {
        if (!isEnabled()) return
        if (!inInventory) return
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!inInventory) return
        val slots = InventoryUtils.getItemsInOpenChest()
        val itemNames = slots.map { it.stack.displayName.removeColor() }
        if (itemNames != inventoryItemNames) {
            inventoryItemNames = itemNames
            inExcavatorMenu = itemNames.any { it == "Start Excavator" }
            if (inExcavatorMenu) return
            // todo remove on merge
            ChatUtils.chat("Inventory update detected")
            updateData()
        }
    }

    private fun updateData() {
        val fossilLocations = mutableSetOf<Int>()
        val dirtLocations = mutableSetOf<Int>()

        var foundChargesRemaining = false
        for (slot in InventoryUtils.getItemsInOpenChest()) {
            val stack = slot.stack
            val slotIndex = slot.slotIndex
            val stackName = stack.displayName.removeColor()
            val isDirt = stackName == "Dirt"
            val isFossil = stackName == "Fossil"
            when {
                isDirt -> dirtLocations.add(slotIndex)
                isFossil -> fossilLocations.add(slotIndex)
                else -> continue
            }

            if (!foundChargesRemaining) {
                for (line in stack.getLore()) {
                    chargesRemainingPattern.matchMatcher(line.removeColor()) {
                        chargesRemaining = group("charges").toInt()
                        foundChargesRemaining = true
                    }
                }
            }

            if (!isFossil || foundPercentage) continue
            for (line in stack.getLore()) {
                fossilProgressPattern.matchMatcher(line.removeColor()) {
                    foundPercentage = true
                    percentage = group("progress")
                }
            }
        }

        coroutineScope.launch {
            FossilExcavatorSolver.findBestTile(fossilLocations, dirtLocations, percentage)
        }
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return
        if (!inInventory) return
        if (inExcavatorMenu) return

        event.makePickblock()

        val slot = event.slot ?: return
        if (slot.slotIndex == slotToClick) {
            slotToClick = null
            correctPercentage = null
        }
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        if (!inInventory) return
        if (inExcavatorMenu) return
        if (slotToClick == null) return

        for (slot in InventoryUtils.getItemsInOpenChest()) {
            if (slot.slotIndex == slotToClick) {
                slot highlight LorenzColor.GREEN
            }
        }
    }

    @SubscribeEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!isEnabled()) return
        if (!inInventory) return
        if (slotToClick != event.slot.slotNumber) return
        if (inExcavatorMenu) return
        val correctPercentage = correctPercentage ?: return

        event.stackTip = correctPercentage
        event.offsetX = 10
        event.offsetY = 10
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!inInventory) return

        if (inExcavatorMenu) {
            // render here so they can move it around. As if you press key while doing the excavator you lose the scrap
            config.position.renderString("§eExcavator solver gui", posLabel = "Fossil Excavator")
            return
        }

        val displayString = when {
            isNotPossible -> NOT_POSSIBLE_STRING
            isCompleted -> SOLVED_STRING
            else -> "$FOSSILS_REMAINING_STRING§a$possibleFossilsRemaining"
        }

        config.position.renderString(displayString, posLabel = "Fossil Excavator")
    }

    fun nextData(slotToClick: FossilTile, correctPercentage: Double, fossilsRemaining: Int) {
        val formattedPercentage = (correctPercentage * 100).round(1)

        this.possibleFossilsRemaining = fossilsRemaining
        this.slotToClick = slotToClick.toSlotIndex()
        this.correctPercentage = "§2$formattedPercentage%"
    }

    fun showError() {
        isNotPossible = true
    }

    fun showCompleted() {
        isCompleted = true
    }

    private fun isEnabled() = IslandType.DWARVEN_MINES.isInIsland() && config.enabled
}
