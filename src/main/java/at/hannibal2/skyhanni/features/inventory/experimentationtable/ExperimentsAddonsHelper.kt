package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.ExperimentationTableApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.EnchantmentsCompat
import at.hannibal2.skyhanni.utils.compat.getIdentifierString
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable
import com.google.gson.JsonPrimitive
import net.minecraft.item.ItemStack

@SkyHanniModule
object ExperimentsAddonsHelper {

    private enum class HelperPhase {
        READ,
        REPLICATE
    }

    private const val ROUND_STATUS_SLOT = 4
    private const val PHASE_STATUS_SLOT = 49

    private val debugConfig get() = SkyHanniMod.feature.dev.debug
    private val config get() = SkyHanniMod.feature.inventory.experimentationTable.addons
    private val hypixelChronomatronData: MutableList<LorenzColor> = mutableListOf()
    private val userChronomatronProgress: MutableList<LorenzColor> = mutableListOf()
    private val hypixelUltrasequencerData: MutableList<Int> = mutableListOf()
    private val userUltrasequencerProgress: MutableList<Int> = mutableListOf()
    private val ultrasequencerDyeMap: MutableMap<Int, ItemStack> = mutableMapOf()

    private var chronHasBeenEmpty: Boolean = false
    private var lastChronomatronSound: SimpleTimeMark = SimpleTimeMark.farPast()
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
        "§7Round: §e(?<round>\\d+)",
    )

    /**
     * REGEX-TEST: §7Timer: §a3s
     * REGEX-TEST: §7Timer: §a10s
     */
    private val replicatePhaseItemPattern by ExperimentationTableApi.patternGroup.pattern(
        "addons.replicate-phase-item",
        "§7Timer: §a\\d+s",
    )

    private val readPhaseItemPattern by ExperimentationTableApi.patternGroup.pattern(
        "addons.read-phase-item",
        "§aRemember the pattern!",
    )

    /**
     * REGEX-TEST: minecraft:stained_hardened_clay
     * REGEX-TEST: minecraft:orange_terracotta
     */
    private val nextChronomatronItemPattern by ExperimentationTableApi.patternGroup.pattern(
        "addons.chronomatron.read-item",
        "(?:minecraft:)?(?:stained_hardened_clay|\\w+_terracotta)",
    )
    // </editor-fold>

    @HandleEvent(InventoryCloseEvent::class, onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun resetAddonsData() {
        hypixelChronomatronData.clear()
        userChronomatronProgress.clear()
        hypixelUltrasequencerData.clear()
        userUltrasequencerProgress.clear()
        currentChronomatronRound = 0
        currentUltraSequencerRound = 0
        chronomatronSequenceIndex = 0
        lastChronomatronSound = SimpleTimeMark.farPast()
        currentAddonPhase = null
        chronHasBeenEmpty = false
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
    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!config.enabled) return
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
            val slotColor = if (color == nextColor) config.nextColor else config.secondColor
            slot.highlight(slotColor)
        }
    }
    // </editor-fold>

    // <editor-fold desc="Slot click stuff">
    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!config.enabled) return
        if (event.slot == null || event.item == null || !ExperimentationTableApi.inAddon) return
        if (currentAddonPhase != HelperPhase.REPLICATE) return
        event.handleChronomatronClick()
        event.handleUltrasequencerClick()
    }

    private fun GuiContainerEvent.SlotClickEvent.handleChronomatronClick() {
        if (!ExperimentationTableApi.inChronomatron || slot == null) return
        if (userChronomatronProgress.size == hypixelChronomatronData.size) return
        val clickedColor = item?.getLorenzColorOrNull()?.takeIf {
            it == hypixelChronomatronData[userChronomatronProgress.size]
        } ?: run {
            if (config.preventMisclicks) cancel()
            return
        }
        userChronomatronProgress.add(clickedColor)
        makePickblock()
    }

    private fun GuiContainerEvent.SlotClickEvent.handleUltrasequencerClick() {
        if (!ExperimentationTableApi.inUltrasequencer || slot == null) return
        if (userUltrasequencerProgress.size == hypixelUltrasequencerData.size) return
        val clickedSlot = slot.slotNumber.takeIf {
            val expectedSlot = hypixelUltrasequencerData[userUltrasequencerProgress.size]
            it == expectedSlot
        } ?: run {
            if (config.preventMisclicks) cancel()
            return
        }
        userUltrasequencerProgress.add(clickedSlot)
        makePickblock()
    }
    // </editor-fold>

    // <editor-fold desc="Next click highlighting">
    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onReplaceItem(event: ReplaceItemEvent) {
        if (!config.enabled) return
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
            hypixelUltrasequencerData.indexOfFirst { it == slot } + 1,
        ] ?: return
        replace(newItem)
    }
    // </editor-fold>

    // <editor-fold desc="Inventory Update reading logic">
    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onPlaySound(event: PlaySoundEvent) {
        if (!ExperimentationTableApi.inChronomatron) return
        // This sound indicates when the player has finished a round in chronomatron
        if (event.soundName != "random.levelup" || event.pitch != 1.7619047f || event.volume != 0.7f) return
        lastChronomatronSound = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
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
        currentChronomatronRound = readChronomatronRoundOrNull() ?: return
        val hypixelSizeNow = hypixelChronomatronData.size
        val userSizeNow = userChronomatronProgress.size

        val activeColors = inventoryItems.values.filter {
            nextChronomatronItemPattern.matches(it.item.getIdentifierString())
        }.mapNotNull { it.getLorenzColorOrNull() }.distinct()

        chronHasBeenEmpty = if (activeColors.isEmpty()) true
        else if (!chronHasBeenEmpty) return
        else false

        val clickedColor = activeColors.firstOrNull { itemColor ->
            val expectedColor = hypixelChronomatronData.getOrNull(chronomatronSequenceIndex)
            expectedColor == null || itemColor == expectedColor
        } ?: return

        val shouldReadLastReplicate = oldPhase == HelperPhase.READ || hypixelSizeNow < currentChronomatronRound
        val isReadingReady = oldPhase == null || oldPhase == HelperPhase.READ
        val shouldNotReadYet = when (currentAddonPhase) {
            HelperPhase.REPLICATE -> !shouldReadLastReplicate
            HelperPhase.READ -> !isReadingReady
            // User hasn't progressed enough or the last sound was too long ago
            else -> userSizeNow < hypixelSizeNow || lastChronomatronSound.isFarPast() && chronomatronSequenceIndex != 0
        }
        if (shouldNotReadYet) return

        // Only record if we're exactly at the next slot, otherwise increment the index
        if (chronomatronSequenceIndex == hypixelSizeNow) {
            hypixelChronomatronData.add(clickedColor)
            lastChronomatronSound = SimpleTimeMark.farPast()
            chronomatronSequenceIndex = 0
            userChronomatronProgress.clear()
        } else chronomatronSequenceIndex++
    }

    private data class UltraSequencerSlot(
        val sequenceNumber: Int,
        val slotIndex: Int,
        val itemStack: ItemStack,
    )

    private fun InventoryUpdatedEvent.readUltrasequencer() {
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

        val isOld = currentUltraSequencerRound != orderedUltrasequencerSlots.size
        val alreadyKnown = hypixelChronomatronData.size == orderedUltrasequencerSlots.size
        if (isOld || alreadyKnown) return

        hypixelUltrasequencerData.clear()
        userUltrasequencerProgress.clear()
        hypixelUltrasequencerData.addAll(orderedUltrasequencerSlots.map { it.slotIndex })
    }
    // </editor-fold>

    // <editor-fold desc="Debugging">
    private fun formatColorSet(list: List<LorenzColor>): String =
        list.joinToString(", ") { it.toString().substring(0, 3) }

    init {
        RenderDisplayHelper(
            inventory = ExperimentationTableApi.experimentationTableInventory,
            condition = { ExperimentationTableApi.inAddon && debugConfig.addonsDebug },
            onlyOnIsland = IslandType.PRIVATE_ISLAND,
            onRender = {
                val renderable = VerticalContainerRenderable(
                    buildList {
                        addString("Current Addon Phase: $currentAddonPhase")
                        if (ExperimentationTableApi.inChronomatron) {
                            addString("Current Round: $currentChronomatronRound")
                            addString("Current Sequence Index: $chronomatronSequenceIndex")
                            addString("")
                            addString("Hypixel Data:")
                            addString(formatColorSet(hypixelChronomatronData))
                            addString("")
                            addString("User Progress:")
                            addString(formatColorSet(userChronomatronProgress))
                            addString("")
                            addString("Last Sound: $lastChronomatronSound")
                        } else if (ExperimentationTableApi.inUltrasequencer) {
                            addString("Current Round: $currentUltraSequencerRound")
                            addString("")
                            addString("Hypixel Data: $hypixelUltrasequencerData")
                            addString("User Progress: $userUltrasequencerProgress")
                            addString("Dye Map: $ultrasequencerDyeMap")
                        } else return@buildList
                    },
                )

                debugConfig.addonsDebugPosition.renderRenderable(renderable, posLabel = "Addons Debug")
            },
        )
    }
    // </editor-fold>

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        val basePath = "inventory.experimentationTable.addons"
        event.move(94, "$basePath.highlightNextClick", "$basePath.enabled")
        event.transform(94, "$basePath.highlightNextClick") {
            JsonPrimitive(true)
        }
        event.transform(94, "$basePath.preventMisclicks") {
            JsonPrimitive(true)
        }
    }
}
