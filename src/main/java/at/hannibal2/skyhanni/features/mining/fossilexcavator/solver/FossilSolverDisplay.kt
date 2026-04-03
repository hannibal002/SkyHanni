package at.hannibal2.skyhanni.features.mining.fossilexcavator.solver

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.SkyHanniMod.launchCoroutine
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.features.mining.fossilexcavator.FossilExcavatorApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.coroutines.CoroutineConfig
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object FossilSolverDisplay {

    private val config get() = SkyHanniMod.feature.mining.fossilExcavator.solver

    private val labelRenderable by lazy { Renderable.text("§eExcavator solver GUI") }

    private val patternGroup = RepoPattern.group("mining.fossilexcavator")

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: Chisel Charges Remaining: 3
     */
    private val chargesRemainingPattern by patternGroup.pattern(
        "chargesremaining",
        "Chisel Charges Remaining: (?<charges>\\d+)",
    )

    /**
     * REGEX-TEST: Fossil Excavation Progress: 8.3%
     */
    private val fossilProgressPattern by patternGroup.pattern(
        "fossilprogress",
        "Fossil Excavation Progress: (?<progress>[\\d.]+%)",
    )
    // </editor-fold desc="Patterns">

    private val inExcavatorMenu get() = FossilExcavatorApi.inExcavatorMenu

    private var foundPercentage = false
    private var percentage: String? = null

    var maxCharges = 0
        private set
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
    private const val CHARGES_REMAINING_STRING = "§eCharges remaining: "

    var possibleFossilTypes = emptySet<FossilType>()

    @HandleEvent
    fun onWorldChange() {
        clearData()
    }

    @HandleEvent
    fun onInventoryClose() {
        clearData()
    }

    // Todo reshape to a data class, use Resettable
    private fun clearData() {
        foundPercentage = false
        percentage = null
        chargesRemaining = 0
        slotToClick = null
        correctPercentage = null
        isNotPossible = false
        isCompleted = false
        inventoryItemNames = emptyList()
        possibleFossilTypes = emptySet()
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onTick() {
        if (!isEnabled()) return
        val slots = InventoryUtils.getItemsInOpenChest()
        val itemNames = slots.map { it.item.hoverName.string.removeColor() }
        if (itemNames != inventoryItemNames) {
            inventoryItemNames = itemNames
            if (inExcavatorMenu) return

            updateData()
        }
    }

    private fun updateData() {
        val fossilLocations = mutableSetOf<Int>()
        val dirtLocations = mutableSetOf<Int>()

        var foundChargesRemaining = false
        for (slot in InventoryUtils.getItemsInOpenChest()) {
            val stack = slot.item
            val slotIndex = slot.containerSlot
            val stackName = stack.hoverName.string.removeColor()
            val isDirt = stackName == "Dirt"
            val isFossil = stackName == "Fossil"
            when {
                isDirt -> dirtLocations.add(slotIndex)
                isFossil -> fossilLocations.add(slotIndex)
                else -> continue
            }

            if (!foundChargesRemaining) {
                for (line in stack.getCleanLore()) {
                    chargesRemainingPattern.matchMatcher(line) {
                        chargesRemaining = group("charges").toInt()
                        if (maxCharges == 0) maxCharges = chargesRemaining
                        foundChargesRemaining = true
                    }
                }
            }

            if (!isFossil || foundPercentage) continue
            for (line in stack.getCleanLore()) {
                fossilProgressPattern.matchMatcher(line) {
                    foundPercentage = true
                    percentage = group("progress")
                }
            }
        }

        CoroutineConfig("fossil solver findBestTile").launchCoroutine {
            FossilSolver.findBestTile(fossilLocations, dirtLocations, percentage)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return
        if (inExcavatorMenu) return

        event.makePickblock()

        val slot = event.slot ?: return
        if (slot.containerSlot == slotToClick) {
            slotToClick = null
            correctPercentage = null
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onForegroundDrawn() {
        if (!isEnabled()) return
        if (inExcavatorMenu) return
        if (slotToClick == null) return

        InventoryUtils.getItemsInOpenChest()
            .firstOrNull { it.containerSlot == slotToClick }
            ?.highlight(LorenzColor.GREEN.toColor().addAlpha(90))
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!isEnabled()) return
        if (!config.showPercentage) return
        if (slotToClick != event.slot.index) return
        if (inExcavatorMenu) return
        val correctPercentage = correctPercentage ?: return

        event.stackTip = correctPercentage
        event.offsetX = 10
        event.offsetY = 10
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onChestGuiRender() {
        if (!isEnabled()) return

        // Render here so they can move it around. As if you press key while doing the excavator you lose the scrap
        if (inExcavatorMenu) return config.position.renderRenderable(labelRenderable, posLabel = "Fossil Excavator Solver")

        val displayList = buildList {
            when {
                isNotPossible -> add(NOT_POSSIBLE_STRING)
                isCompleted -> add(SOLVED_STRING)
                else -> add("${FOSSILS_REMAINING_STRING}§a$possibleFossilsRemaining")
            }
            add("${CHARGES_REMAINING_STRING}§a$chargesRemaining")

            if (possibleFossilTypes.isNotEmpty()) {
                add("§ePossible Fossil types:")
                for (fossil in possibleFossilTypes) {
                    add("§7- ${fossil.displayName}")
                }
            }
        }.map(Renderable::text)

        config.position.renderRenderables(displayList, posLabel = "Fossil Excavator Solver")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(36, "mining.fossilExcavator", "mining.fossilExcavator2.solver")
        event.move(37, "mining.fossilExcavator2", "mining.fossilExcavator")
    }

    fun nextData(slotToClick: FossilTile, correctPercentage: Double, fossilsRemaining: Int) {
        val formattedPercentage = (correctPercentage * 100).roundTo(1)

        possibleFossilsRemaining = fossilsRemaining
        FossilSolverDisplay.slotToClick = slotToClick.toSlotIndex()
        FossilSolverDisplay.correctPercentage = "§2$formattedPercentage%"
    }

    fun showError() {
        isNotPossible = true
    }

    fun showCompleted() {
        isCompleted = true
    }

    private fun isEnabled() = config.enabled && FossilExcavatorApi.excavatorInventory.isInside()
}
