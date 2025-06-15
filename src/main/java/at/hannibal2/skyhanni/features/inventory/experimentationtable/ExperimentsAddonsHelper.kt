package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.ExperimentationTableApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.EnchantmentsCompat
import at.hannibal2.skyhanni.utils.compat.getIdentifierString
import net.minecraft.item.ItemStack

@SkyHanniModule
object ExperimentsAddonsHelper {

    private enum class HelperPhase { READ, REPLICATE }

    private const val ROUND_STATUS_SLOT = 4
    private const val PHASE_STATUS_SLOT = 49

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable.addons
    private val hypixelChronomatronData: MutableList<LorenzColor> = mutableListOf()
    private val userChronomatronProgress: MutableList<LorenzColor> = mutableListOf()
    private val hypixelUltrasequencerData: MutableList<Int> = mutableListOf()
    private val userUltrasequencerProgress: MutableList<Int> = mutableListOf()
    private val ultrasequencerDyeMap: MutableMap<Int, ItemStack> = mutableMapOf()

    private var currentAddonPhase: HelperPhase? = null
    private var chronomatronSequenceIndex: Int = 0
    var currentChronomatronRound: Int = 0
        private set
    var currentUltraSequencerRound: Int = 0
        private set

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: §7Round: §e1
     * REGEX-TEST: §7Round: §e2
     */
    private val roundItemPattern by ExperimentationTableApi.patternGroup.pattern(
        "addons.round-item",
        "§7Round: §e(?<round>\\d+)"
    )

    /**
     * REGEX-TEST: §7Timer: §a3s
     * REGEX-TEST: §7Timer: §a10s
     */
    private val replicatePhaseItemPattern by ExperimentationTableApi.patternGroup.pattern(
        "addons.replicate-phase-item",
        "§7Timer: §a\\d+s"
    )

    private val readPhaseItemPattern by ExperimentationTableApi.patternGroup.pattern(
        "addons.read-phase-item",
        "§aRemember the pattern!"
    )

    /**
     * REGEX-TEST: minecraft:stained_hardened_clay
     * REGEX-TEST: minecraft:orange_terracotta
     */
    private val nextChronomatronItemPattern by ExperimentationTableApi.patternGroup.pattern(
        "addons.chronomatron.read-item",
        "(?:minecraft:)?(?:stained_hardened_clay|\\w+_terracotta)"
    )
    // </editor-fold>

    @HandleEvent(InventoryCloseEvent::class)
    fun resetAddonsData() {
        hypixelChronomatronData.clear()
        userChronomatronProgress.clear()
        hypixelUltrasequencerData.clear()
        userUltrasequencerProgress.clear()
        currentChronomatronRound = 0
        currentUltraSequencerRound = 0
        chronomatronSequenceIndex = 0
        currentAddonPhase = null
    }

    private fun ItemStack.getLorenzColorOrNull(): LorenzColor? = when (displayName.removeColor()) {
        "Green" -> LorenzColor.DARK_GREEN
        "Lime" -> LorenzColor.GREEN
        "Pink" -> LorenzColor.LIGHT_PURPLE
        "Cyan" -> LorenzColor.DARK_AQUA
        "Orange" -> LorenzColor.GOLD
        "Purple" -> LorenzColor.DARK_PURPLE
        else -> try {
            LorenzColor.valueOf(displayName.removeColor().uppercase())
        } catch (exception: IllegalArgumentException) {
            null
        }
    }

    // <editor-fold desc="Next click highlighting">
    @HandleEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!config.highlightNextClick || currentAddonPhase != HelperPhase.REPLICATE) return

        if (!ExperimentationTableApi.inAddon) return
        if (ExperimentationTableApi.inUltrasequencer && currentUltraSequencerRound >= 1) tryHighlightUltrasequencer()
        if (ExperimentationTableApi.inChronomatron && currentChronomatronRound >= 1) tryHighlightChronomatron()
    }

    private fun tryHighlightUltrasequencer() = InventoryUtils.getItemsInOpenChest().filter {
        it.stack.displayName.trim().isNotEmpty() && it.slotNumber in hypixelUltrasequencerData &&
            hypixelUltrasequencerData.indexOf(it.slotNumber) > (userUltrasequencerProgress.size - 1)
    }.sortedBy {
        hypixelUltrasequencerData.indexOf(it.slotNumber)
    }.forEachIndexed { slotIndex, slot ->
        val alphaValue = (255 / (1 + slotIndex))
        val slotColor = LorenzColor.GREEN.addOpacity(alphaValue)
        slot.highlight(slotColor)
    }

    private fun tryHighlightChronomatron() {
        val nextColor = hypixelChronomatronData.getOrNull(userChronomatronProgress.size)
        val nextNextColor = hypixelChronomatronData.getOrNull(userChronomatronProgress.size + 1)

        InventoryUtils.getItemsInOpenChest().forEach { slot ->
            val color = slot.stack.getLorenzColorOrNull() ?: return@forEach
            if (color !in listOf(nextColor, nextNextColor)) return@forEach
            val alphaValue = if (color == nextColor) 255 else 128
            val slotColor = LorenzColor.GREEN.addOpacity(alphaValue)
            slot.highlight(slotColor)
        }
    }
    // </editor-fold>

    // <editor-fold desc="Slot click stuff">
    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (event.slot == null || event.item == null || !ExperimentationTableApi.inAddon) return
        if (!config.preventMisclicks || currentAddonPhase != HelperPhase.REPLICATE) return
        event.handleChronomatronClick()
        event.handleUltrasequencerClick()
    }

    private fun GuiContainerEvent.SlotClickEvent.handleChronomatronClick() {
        if (!ExperimentationTableApi.inChronomatron || slot == null) return
        if (userChronomatronProgress.size == hypixelChronomatronData.size) return
        val clickedColor = item?.getLorenzColorOrNull()?.takeIf {
            it == hypixelChronomatronData[userChronomatronProgress.size]
        } ?: return cancel()
        userChronomatronProgress.add(clickedColor)
        makePickblock()
    }

    private fun GuiContainerEvent.SlotClickEvent.handleUltrasequencerClick() {
        if (!ExperimentationTableApi.inUltrasequencer || slot == null) return
        if (userUltrasequencerProgress.size == hypixelUltrasequencerData.size) return
        val clickedSlot = slot.slotNumber.takeIf {
            val expectedSlot = hypixelUltrasequencerData[userUltrasequencerProgress.size]
            it == expectedSlot
        } ?: return cancel()
        userUltrasequencerProgress.add(clickedSlot)
        makePickblock() // Prevents a flashbang
    }
    // </editor-fold>

    // <editor-fold desc="Next click highlighting">
    @HandleEvent
    fun onReplaceItem(event: ReplaceItemEvent) {
        if (!ExperimentationTableApi.inAddon || !config.highlightNextClick || currentAddonPhase != HelperPhase.REPLICATE) return

        if (ExperimentationTableApi.inChronomatron) event.replaceChronomatronItem()
        if (ExperimentationTableApi.inUltrasequencer) event.replaceUltrasequencerItems()
    }

    private fun ReplaceItemEvent.replaceChronomatronItem() {
        val nextClickColor = hypixelChronomatronData.getOrNull(userChronomatronProgress.size) ?: return
        originalItem.getLorenzColorOrNull()?.takeIf { it == nextClickColor } ?: return
        val newItem = originalItem.copy()
        newItem.addEnchantment(EnchantmentsCompat.PROTECTION.enchantment, 1)
        replace(newItem)
    }

    private fun ReplaceItemEvent.replaceUltrasequencerItems() {
        val newItem = ultrasequencerDyeMap[
            hypixelUltrasequencerData.indexOfFirst { it == slot } + 1
        ] ?: return
        replace(newItem)
    }
    // </editor-fold>

    // <editor-fold desc="Inventory Update reading logic">
    @HandleEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!ExperimentationTableApi.inAddon) return

        val oldAddonPhase = currentAddonPhase
        currentAddonPhase = event.readPhaseOrNull() ?: return

        if (ExperimentationTableApi.inChronomatron) event.readNextChronomatron(oldAddonPhase)
        if (ExperimentationTableApi.inUltrasequencer) event.readUltrasequencer()
    }

    private fun InventoryUpdatedEvent.readPhaseOrNull(): HelperPhase? {
        val phaseItemName = inventoryItems[PHASE_STATUS_SLOT]?.displayName ?: return null
        return when {
            replicatePhaseItemPattern.matches(phaseItemName) -> HelperPhase.REPLICATE
            readPhaseItemPattern.matches(phaseItemName) -> HelperPhase.READ
            else -> null
        }
    }

    private fun InventoryUpdatedEvent.readChronomatronRoundOrNull(): Int? {
        val roundItemName = inventoryItems[ROUND_STATUS_SLOT]?.displayName ?: return null
        return roundItemPattern.matchGroup(roundItemName, "round")?.formatIntOrNull()
    }

    private fun InventoryUpdatedEvent.readNextChronomatron(oldPhase: HelperPhase? = null) {
        val oldChronomatronRound = currentChronomatronRound
        currentChronomatronRound = readChronomatronRoundOrNull() ?: return
        if (currentChronomatronRound != oldChronomatronRound) {
            chronomatronSequenceIndex = 0
            userChronomatronProgress.clear()
        }

        val shouldReadLastReplicate = oldPhase == HelperPhase.READ || hypixelChronomatronData.size < currentChronomatronRound
        val isReadingReady = oldPhase == null || oldPhase == HelperPhase.READ
        val shouldEarlyReturn = when (currentAddonPhase) {
            HelperPhase.REPLICATE -> !shouldReadLastReplicate
            HelperPhase.READ -> !isReadingReady
            else -> true
        }
        if (shouldEarlyReturn) return

        val clickedColor = inventoryItems.values.firstOrNull {
            nextChronomatronItemPattern.matches(it.item.getIdentifierString())
        }?.getLorenzColorOrNull()?.takeIf { itemColor ->
            val expectedColor = hypixelChronomatronData.getOrNull(chronomatronSequenceIndex)
            expectedColor == null || itemColor == expectedColor
        } ?: return

        // Only record if we're exactly at the next slot, otherwise increment the index
        if (chronomatronSequenceIndex == hypixelChronomatronData.size) hypixelChronomatronData.add(clickedColor)
        else chronomatronSequenceIndex++
    }

    private data class UltraSequencerSlot(
        val sequenceNumber: Int,
        val slotIndex: Int,
        val itemStack: ItemStack,
    )

    private fun InventoryUpdatedEvent.readUltrasequencer() {
        if (currentAddonPhase != HelperPhase.READ) return
        hypixelUltrasequencerData.clear()

        val orderedUltrasequencerSlots = inventoryItems.filter {
            it.value.displayName.trim().isNotEmpty()
        }.mapNotNull { (slot, stack) ->
            val sequenceNumber = stack.displayName.removeColor().toIntOrNull() ?: return@mapNotNull null
            currentUltraSequencerRound = maxOf(currentUltraSequencerRound, sequenceNumber)
            if (sequenceNumber !in ultrasequencerDyeMap) ultrasequencerDyeMap[sequenceNumber] = stack
            UltraSequencerSlot(
                sequenceNumber = sequenceNumber,
                slotIndex = slot,
                itemStack = stack,
            )
        }.sortedBy { it.sequenceNumber }

        userUltrasequencerProgress.clear()
        hypixelUltrasequencerData.addAll(orderedUltrasequencerSlots.map { it.slotIndex })
    }
    // </editor-fold>
}
