package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentsAddonsHelper.getLorenzColorOrNull
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.getIdentifierString
import net.minecraft.item.ItemStack

@SkyHanniModule
object ExperimentsAddonsHelper {

    private enum class HelperPhase { READ, REPLICATE }
    private const val ROUND_STATUS_SLOT = 4
    private const val PHASE_STATUS_SLOT = 49
    private val config get() = SkyHanniMod.feature.inventory.experimentationTable.addonHelpers
    private val addonsTypes = listOf(TaskType.CHRONOMATRON, TaskType.ULTRASEQUENCER)

    private val socChronomatron: MutableList<LorenzColor> = mutableListOf()
    private val userChronomatron: MutableList<LorenzColor> = mutableListOf()

    private var currentPhase: HelperPhase? = null
    private var currentRound: Int = 0
    private var lastClickedUserSlot: Int = 0

    private var chronomatronSequenceIndex: Int = 0

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

    private fun reset() {
        socChronomatron.clear()
        userChronomatron.clear()
        //socUltrasequencer.clear()
        currentRound = 0
        currentPhase = null
        chronomatronSequenceIndex = 0
    }

    @HandleEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (ExperimentationTableApi.currentExperimentType != TaskType.CHRONOMATRON) return

        Position(-500, 300).renderStrings(
            buildList {
                add("Chronomatron Colors")
                addAll(
                    socChronomatron.map { it.toString() }
                )
            },
            posLabel = "Chronomatron Colors"
        )

        Position(-300, 300).renderStrings(
            buildList {
                add("User Chronomatron Colors")
                addAll(
                    userChronomatron.map { it.toString() }
                )
            },
            posLabel = "User Chronomatron Colors"
        )
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        reset()
    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (ExperimentationTableApi.currentExperimentType !in addonsTypes) return
        if (currentPhase != HelperPhase.REPLICATE) return
        if (event.item == null) return

        event.handleChronomatronClick()
    }

    private fun GuiContainerEvent.SlotClickEvent.handleChronomatronClick() {
        val clickedColor = item?.getLorenzColorOrNull()?.takeIf {
            it ==  socChronomatron[userChronomatron.size]
        } ?: return cancel()
        userChronomatron.add(clickedColor)
    }

    @HandleEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (ExperimentationTableApi.currentExperimentType !in addonsTypes) return

        val oldPhase = currentPhase
        currentPhase = event.readPhaseOrNull() ?: return

        val oldRound = currentRound
        currentRound = event.readRoundOrNull() ?: return
        if (currentRound != oldRound) {
            chronomatronSequenceIndex = 0
            userChronomatron.clear()
        }

        event.readAddonData(oldPhase)
    }

    private fun InventoryUpdatedEvent.readRoundOrNull(): Int? {
        val roundItemName = inventoryItems[ROUND_STATUS_SLOT]?.displayName ?: return null
        roundItemPattern.matchMatcher(roundItemName) {
            return group("round").formatIntOrNull()
        }
        return null
    }

    private fun InventoryUpdatedEvent.readPhaseOrNull(): HelperPhase? {
        val phaseItemName = inventoryItems[PHASE_STATUS_SLOT]?.displayName ?: return null
        return when {
            replicatePhaseItemPattern.matches(phaseItemName) -> HelperPhase.REPLICATE
            readPhaseItemPattern.matches(phaseItemName) -> HelperPhase.READ
            else -> null
        }
    }

    private fun InventoryUpdatedEvent.readAddonData(oldPhase: HelperPhase? = null) {
        when (ExperimentationTableApi.currentExperimentType) {
            TaskType.CHRONOMATRON -> readNextChronomatron(oldPhase)
            TaskType.ULTRASEQUENCER -> readUltrasequencer()
            else -> return
        }
    }

    private fun InventoryUpdatedEvent.readNextChronomatron(oldPhase: HelperPhase? = null) {
        val shouldReadLastReplicate = oldPhase == HelperPhase.READ || socChronomatron.size < currentRound
        if (currentPhase == HelperPhase.REPLICATE && !shouldReadLastReplicate) return

        val isReadingReady = oldPhase == null || oldPhase == HelperPhase.READ
        if (currentPhase == HelperPhase.READ && !isReadingReady) return

        val highlightedItem = inventoryItems.values.firstOrNull {
            nextChronomatronItemPattern.matches(it.item.getIdentifierString())
        } ?: return

        val color = highlightedItem.getLorenzColorOrNull() ?: return

        val expectedIndexColor = if (socChronomatron.size < chronomatronSequenceIndex + 1) null
        else socChronomatron[chronomatronSequenceIndex]
        if (expectedIndexColor != null && expectedIndexColor != color) return

        // Only record if we're exactly at the next slot
        if (chronomatronSequenceIndex == socChronomatron.size) socChronomatron.add(color)
        else chronomatronSequenceIndex++
    }

    private fun ItemStack.getLorenzColorOrNull(): LorenzColor? = when (displayName.removeColor()) {
        "Green" -> at.hannibal2.skyhanni.utils.LorenzColor.DARK_GREEN
        "Lime" -> at.hannibal2.skyhanni.utils.LorenzColor.GREEN
        "Pink" -> at.hannibal2.skyhanni.utils.LorenzColor.LIGHT_PURPLE
        "Cyan" -> at.hannibal2.skyhanni.utils.LorenzColor.DARK_AQUA
        "Orange" -> at.hannibal2.skyhanni.utils.LorenzColor.GOLD
        "Purple" -> at.hannibal2.skyhanni.utils.LorenzColor.DARK_PURPLE
        else -> try {
            LorenzColor.valueOf(displayName.removeColor().uppercase())
        } catch (exception: IllegalArgumentException) {
            null
        }
    }

    private fun InventoryUpdatedEvent.readUltrasequencer() {

    }

}
