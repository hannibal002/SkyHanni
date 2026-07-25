package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.experiments.TableSuperpairDataUpdatedEvent
import at.hannibal2.skyhanni.events.experiments.TableTaskStartedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.DyeCompat
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets

@SkyHanniModule
object ExperimentationSuperpairApi {

    data class SuperpairItem(val slotId: Int, val reward: String, val damage: Int)
    data class FoundData(
        val item: SuperpairItem? = null,
        val first: SuperpairItem? = null,
        val second: SuperpairItem? = null,
    )

    enum class FoundType {
        NORMAL,
        POWERUP,
        MATCH,
        PAIR,
    }

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: GUARDIAN;4
     */
    private val guardianPetInternalNamePattern by ExperimentationTableApi.patternGroup.pattern(
        "guardian.pet.internalname",
        "GUARDIAN;\\d"
    )

    /**
     * REGEX-TEST: §8?
     * REGEX-TEST: §eClick any button!
     * REGEX-TEST: §bClick a second button!
     * REGEX-TEST: §dNext button is instantly rewarded!
     */
    val unknownSuperpairsClickPattern by ExperimentationTableApi.patternGroup.pattern(
        "superpairs.unknown-click",
        "(?:§.)+(?:\\?|(?:Click a(?: seco)?n[dy]|Next) button(?: is instantly rewarded)?!?)"
    )

    /**
     * REGEX-TEST: Superpairs Powerup
     */
    private val powerupLorePattern by ExperimentationTableApi.patternGroup.pattern(
        "powerups.lore",
        "(?i).*powerup.*",
    )

    /**
     * REGEX-TEST: Remaining Clicks: 22
     * REGEX-TEST: Remaining Clicks: 0
     */
    private val remainingClicksPattern by ExperimentationTableApi.patternGroup.pattern(
        "clicks",
        "Remaining Clicks: (?<clicks>\\d+)",
    )
    // </editor-fold>

    private const val CLICKS_SLOT = 4

    private val superpairsSlotMap: MutableMap<Int, SafeItemStack> = mutableMapOf()
    private var lastClickedSlot = -1
    private var currentFoundData = mapOf<FoundType, List<FoundData>>()

    // True from the second consecutive "Remaining Clicks: 0" reading onward, skipping the
    // round-end reveal of leftover cards while still processing the genuine final click.
    // Resets if clicks are seen above 0 again (e.g. a +Clicks powerup).
    private var frozen = false

    val foundData: Map<FoundType, List<FoundData>> get() = currentFoundData
    fun found(type: FoundType): List<FoundData> = currentFoundData[type].orEmpty()

    val uncoveredItemStacks: Map<Int, SafeItemStack> get() = superpairsSlotMap

    @HandleEvent
    fun onInventoryClose() = reset()

    @HandleEvent
    fun onTableTaskStarted(event: TableTaskStartedEvent) = reset()

    private fun reset() {
        superpairsSlotMap.clear()
        currentFoundData = emptyMap()
        lastClickedSlot = -1
        frozen = false
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!ExperimentationTableApi.inSuperpairs) return
        val tier = ExperimentationTableApi.currentExperimentTier ?: return
        if (event.slotId in tier.slotRange) lastClickedSlot = event.slotId
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!ExperimentationTableApi.inSuperpairs) return
        if (ExperimentationTableApi.expOverInventoryPattern.matches(event.inventoryName)) return
        val tier = ExperimentationTableApi.currentExperimentTier ?: return

        val remainingClicks = event.inventoryItems[CLICKS_SLOT]?.let { clicksItem ->
            remainingClicksPattern.matchMatcher(clicksItem.cleanName) { group("clicks").toIntOrNull() }
        }
        val wasFrozen = frozen
        frozen = remainingClicks == 0
        if (frozen && wasFrozen) return

        scan(event.inventoryItems, tier)
    }

    private fun scan(inventoryItems: Map<Int, SafeItemStack>, tier: ExperimentationTableApi.ExperimentationTier) {
        val revealed = mutableMapOf<Int, SafeItemStack>()
        val hiddenSlots = mutableListOf<Int>()
        for (slot in tier.slotRange) {
            val stack = inventoryItems[slot] ?: continue
            if (stack.isHiddenCard()) hiddenSlots.add(slot)
            else {
                revealed[slot] = stack
                superpairsSlotMap[slot] = stack
            }
        }

        val powerups = revealed.filterValues { it.isPowerup() }.map { (slot, stack) -> stack.toSuperpairItem(slot) }

        // Collected pairs stay face-up permanently. A freshly clicked card is also face-up,
        // so with an odd count per reward the last clicked slot is excluded to not pair a
        // fresh reveal with an identical already collected pair.
        val revealedCards = revealed.filterValues { !it.isPowerup() }.map { (slot, stack) -> stack.toSuperpairItem(slot) }
        val pairs = mutableListOf<FoundData>()
        for (group in revealedCards.groupBy { it.reward to it.damage }.values) {
            val cards = if (group.size % 2 == 1) group.filterNot { it.slotId == lastClickedSlot } else group
            cards.chunked(2).forEach { if (it.size == 2) pairs.add(FoundData(first = it[0], second = it[1])) }
        }

        // Face-down cards remembered from earlier reveals: full pairs are known matches,
        // leftovers are cards seen once whose partner is still unknown
        val rememberedHidden = hiddenSlots.mapNotNull { slot -> superpairsSlotMap[slot]?.toSuperpairItem(slot) }
        val matches = mutableListOf<FoundData>()
        val normals = mutableListOf<FoundData>()
        for (group in rememberedHidden.groupBy { it.reward to it.damage }.values) {
            group.chunked(2).forEach {
                if (it.size == 2) matches.add(FoundData(first = it[0], second = it[1]))
                else normals.add(FoundData(item = it[0]))
            }
        }

        val newData = buildMap {
            if (pairs.isNotEmpty()) put(FoundType.PAIR, pairs.toList())
            if (matches.isNotEmpty()) put(FoundType.MATCH, matches.toList())
            if (normals.isNotEmpty()) put(FoundType.NORMAL, normals.toList())
            if (powerups.isNotEmpty()) put(FoundType.POWERUP, powerups.map { FoundData(item = it) })
        }
        if (newData == currentFoundData) return
        currentFoundData = newData
        TableSuperpairDataUpdatedEvent().post()
    }

    private fun SafeItemStack.isHiddenCard() =
        unknownSuperpairsClickPattern.matches(hoverName.formattedTextCompatLeadingWhiteLessResets())

    private fun SafeItemStack.isPowerup() = powerupLorePattern.anyMatches(getLore())

    private fun SafeItemStack.toSuperpairItem(slot: Int) = SuperpairItem(slot, convertToReward(), DyeCompat.toDamage(this))

    private fun SafeItemStack.convertToReward() = when {
        guardianPetInternalNamePattern.matches(getInternalNameOrNull()?.asString().orEmpty()) ->
            hoverName.formattedTextCompatLeadingWhiteLessResets().split("] ")[1]

        cleanName == "Enchanted Book" -> getLore().getOrNull(2)?.removeColor() ?: "Enchanted Book"
        else -> cleanName
    }
}
