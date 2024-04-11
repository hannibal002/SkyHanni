package at.hannibal2.skyhanni.features.mining.fossilexcavator

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.SkyHanniMod.Companion.coroutineScope
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
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
    private var foundPercentage = false
    private var percentage: String? = null
    private var chargesRemaining = 0
    private var slotToClick: Int? = null
    private var correctPercentage: String? = null
    private var isNotPossible = false

    private const val NOT_POSSIBLE_STRING = "§cNo possible fossils on board."

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
        foundPercentage = false
        percentage = null
        chargesRemaining = 0
        slotToClick = null
        correctPercentage = null
        isNotPossible = false
    }

    @SubscribeEvent
    fun onInventoryUpdate(event: InventoryUpdatedEvent) {
        if (!isEnabled()) return
        if (!inInventory) return


    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return
        if (!inInventory) return

        val eventSlot = event.slot ?: return
        if (eventSlot.slotIndex == slotToClick) {
            slotToClick = null
            correctPercentage = null
        }

        val correctItem = eventSlot.stack.name.removeColor() == "Dirt"
        if (!correctItem) return

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
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        if (!inInventory) return
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
        val correctPercentage = correctPercentage ?: return

        event.stackTip = correctPercentage
        event.offsetX = 10
        event.offsetY = 10
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!inInventory) return
        if (isNotPossible) {
            config.position.renderString(NOT_POSSIBLE_STRING, posLabel = "Fossil Excavator")
        }
    }

    fun nextData(slotToClick: FossilTile, correctPercentage: Double) {
        val formattedPercentage = (correctPercentage * 100).round(1)

        this.slotToClick = slotToClick.toSlotIndex()
        this.correctPercentage = "§2$formattedPercentage%"
    }

    fun showError() {
        isNotPossible = true
    }

    fun isEnabled() = IslandType.DWARVEN_MINES.isInIsland() && config.enabled
}
