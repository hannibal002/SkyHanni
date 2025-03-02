package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentationTableApi.remainingClicksPattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils.isAnyOf
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object SuperpairDataDisplay {

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable

    // <editor-fold desc="Patterns">
    private val instantFindNamePattern by ExperimentationTableApi.patternGroup.pattern(
        "powerups.instantfind.name",
        "Instant Find",
    )

    private val waitingMessagesPattern by ExperimentationTableApi.patternGroup.pattern(
        "waiting.messages",
        "Click any button!|Click a second button!|Next button is instantly rewarded!",
    )
    // </editor-fold>

    private data class SuperpairItem(val slotId: Int, val reward: String, val damage: Int)
    private data class FoundData(val item: SuperpairItem? = null, val first: SuperpairItem? = null, val second: SuperpairItem? = null)

    private enum class FoundType {
        NORMAL,
        POWERUP,
        MATCH,
        PAIR
    }

    private val emptySuperpairItem = SuperpairItem(-1, "", -1)

    private var display = emptyList<String>()
    private var uncoveredItems = mapOf<Int, SuperpairItem>()
    private val found = mutableMapOf<FoundType, MutableList<FoundData>>()

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        display = emptyList()
        uncoveredItems = emptyMap()
        found.clear()
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!config.superpairDisplay) return
        if (InventoryUtils.openInventoryName() == "Experimentation Table") {
            config.superpairDisplayPosition.renderString(
                "§6Superpair Experimentation Data",
                posLabel = "Superpair Experimentation Data"
            )
        }
        if (ExperimentationTableApi.currentExperiment == null) return

        if (display.isEmpty()) display = drawDisplay()

        config.superpairDisplayPosition.renderStrings(display, posLabel = "Superpair Experimentation Data")
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!config.superpairDisplay) return
        if (ExperimentationTableApi.currentExperiment == null) return

        val currentExperiment = ExperimentationTableApi.currentExperiment ?: return

        val item = event.item ?: return
        if (isOutOfBounds(event.slotId, currentExperiment) || item.displayName.removeColor() == "?") return

        val clicksItem = InventoryUtils.getItemAtSlotIndex(4)
        val hasRemainingClicks = remainingClicksPattern.matchMatcher(clicksItem?.displayName?.removeColor().orEmpty()) {
            group("clicks").toInt() > 0
        } ?: false

        val items = uncoveredItems.toMutableMap()
        val itemExistsInData = items.any { it.value.slotId == event.slotId && it.key == items.keys.max() }
        if (!itemExistsInData && hasRemainingClicks) handleItem(items, event.slotId)
        uncoveredItems = items
    }

    private fun handleItem(items: MutableMap<Int, SuperpairItem>, slot: Int) = DelayedRun.runDelayed(200.milliseconds) {
        val itemNow = InventoryUtils.getItemAtSlotIndex(slot) ?: return@runDelayed
        val itemName = itemNow.displayName.removeColor()
        val reward = convertToReward(itemNow)
        val itemData = SuperpairItem(slot, reward, itemNow.itemDamage)
        val uncovered = items.keys.maxOrNull() ?: -1

        if (isWaiting(itemName)) return@runDelayed

        if (items.none { it.key == uncovered && it.value.slotId == slot }) items[uncovered + 1] = itemData

        when {
            isPowerUp(reward) -> handlePowerUp(items, itemData, uncovered + 1)
            isReward(itemName) -> handleReward(items, itemData, uncovered + 1)
        }

        val since = clicksSinceSeparator(items)

        val lastReward = items.entries.lastOrNull()?.value?.reward
        if ((since >= 2 || (since == -1 && items.size >= 2)) && !instantFindNamePattern.matches(lastReward)) items[uncovered + 2] =
            emptySuperpairItem

        display = drawDisplay()
    }

    private fun handlePowerUp(items: MutableMap<Int, SuperpairItem>, item: SuperpairItem, uncovered: Int) {
        if (!instantFindNamePattern.matches(item.reward)) items.remove(uncovered)

        val itemData = FoundData(item = item)
        found.getOrPut(FoundType.POWERUP) { mutableListOf(itemData) }.apply { if (!contains(itemData)) add(itemData) }
    }

    private fun handleReward(items: MutableMap<Int, SuperpairItem>, item: SuperpairItem, uncovered: Int) {
        val last = items.getOrDefault(uncovered - 1, item)

        if (isWaiting(last.reward)) return

        when {
            instantFindNamePattern.matches(last.reward) -> handleInstantFind(items, item, uncovered)
            hasFoundPair(item, last) -> handleFoundPair(item, last)
            hasFoundMatch(items, item) -> handleFoundMatch(items, item)
            else -> handleNormalReward(item)
        }
    }

    private fun handleInstantFind(items: MutableMap<Int, SuperpairItem>, item: SuperpairItem, uncovered: Int) {
        items[uncovered - 1] = item
        items[uncovered] = emptySuperpairItem

        handleFoundPair(item, emptySuperpairItem)
    }

    private fun handleFoundPair(
        first: SuperpairItem,
        second: SuperpairItem,
    ) {
        found.entries.forEach {
            when (it.key) {
                FoundType.MATCH -> it.value.removeIf { data -> data.first?.sameAs(first) ?: false || data.second?.sameAs(first) ?: false }
                FoundType.NORMAL -> it.value.removeIf { data -> data.item?.sameAs(first) ?: false || data.item?.sameAs(second) ?: false }
                else -> {}
            }
        }

        val pairData = FoundData(first = first, second = second)

        found.getOrPut(FoundType.PAIR) { mutableListOf(pairData) }.apply { if (!contains(pairData)) add(pairData) }
    }

    private fun handleFoundMatch(
        items: MutableMap<Int, SuperpairItem>,
        item: SuperpairItem
    ) {
        val matchingItem = items.values.find { it.slotId != item.slotId && it.sameAs(item) } ?: return
        val slotIds = listOf(item.slotId, matchingItem.slotId)

        for ((foundType, dataList) in found.entries) {
            when (foundType) {
                FoundType.MATCH, FoundType.PAIR -> {
                    // If any data already contains one of the slot IDs in either first or second, exit.
                    val alreadyExists = dataList.any { it.first?.slotId in slotIds || it.second?.slotId in slotIds }
                    if (alreadyExists) return
                }
                // Remove data where the associated item's slotId is one of the found IDs.
                FoundType.NORMAL -> dataList.removeIf { data -> data.item?.slotId in slotIds }
                else -> {}
            }
        }

        val pairData = FoundData(first = item, second = matchingItem)
        found.getOrPut(FoundType.MATCH) { mutableListOf(pairData) }.apply { if (!contains(pairData)) add(pairData) }
    }

    private fun handleNormalReward(item: SuperpairItem) {
        val slotIds = listOf(item.slotId)

        for ((foundType, dataList) in found.entries) {
            when (foundType) {
                FoundType.MATCH, FoundType.PAIR -> {
                    // If any data already contains one of the slot IDs in either first or second, exit.
                    val alreadyExists = dataList.any { it.first?.slotId in slotIds || it.second?.slotId in slotIds }
                    if (alreadyExists) return
                }
                else -> {
                    val exists = dataList.any { it.item != null && it.item.slotId == item.slotId && it.item.sameAs(item) }
                    if (exists) return
                }
            }
        }

        val itemData = FoundData(item = item)
        found.getOrPut(FoundType.NORMAL) { mutableListOf(itemData) }.apply { if (!contains(itemData)) add(itemData) }
    }

    private fun drawDisplay() = buildList {
        val currentExperiment = ExperimentationTableApi.currentExperiment ?: return emptyList<String>()

        add("§6Superpair Experimentation Data")
        add("")

        val normals = found.entries.firstOrNull { it.key == FoundType.NORMAL }?.value.orEmpty()
        val powerups = found.entries.firstOrNull { it.key == FoundType.POWERUP }?.value.orEmpty()
        val matches = found.entries.firstOrNull { it.key == FoundType.MATCH }?.value.orEmpty()
        val pairs = found.entries.firstOrNull { it.key == FoundType.PAIR }?.value.orEmpty()
        val possiblePairs = calculatePossiblePairs(currentExperiment)

        if (pairs.isNotEmpty()) add("§2Collected")
        for (pair in pairs) {
            val prefix = determinePrefix(pairs.indexOf(pair), pairs.lastIndex)
            add(" $prefix §a${pair.first?.reward.orEmpty()}")
        }
        if (matches.isNotEmpty()) add("§eMatched")
        for (match in matches) {
            val prefix = determinePrefix(matches.indexOf(match), matches.lastIndex)
            add(" $prefix §e${match.first?.reward.orEmpty()}")
        }
        if (powerups.isNotEmpty()) add("§bPowerUp")
        for (powerup in powerups) {
            val prefix = determinePrefix(powerups.indexOf(powerup), powerups.size - 1)
            add(" $prefix §b${powerup.item?.reward.orEmpty()}")
        }
        val toAdd = mutableListOf<String>()
        if (possiblePairs >= 1) toAdd.add("§ePairs - $possiblePairs")
        if (2 - powerups.size >= 1) toAdd.add("§bPowerUps - ${2 - powerups.size}")
        if (normals.isNotEmpty()) toAdd.add("§7Normals - ${normals.size}")

        if (toAdd.isNotEmpty()) {
            add("")
            add("§4Not collected")
        }
        for (string in toAdd) if (string != toAdd.last()) add(" ├ $string") else add(" └ $string")
    }

    private fun calculatePossiblePairs(currentExperiment: ExperimentTier) =
        ((currentExperiment.gridSize - 2) / 2) - found.filter { it.key != FoundType.POWERUP }.values.sumOf { it.size }

    private fun convertToReward(item: ItemStack) = if (item.displayName.removeColor() == "Enchanted Book") item.getLore()[2].removeColor()
    else item.displayName.removeColor()

    private fun determinePrefix(index: Int, lastIndex: Int) = if (index == lastIndex) "└" else "├"

    private fun hasFoundPair(
        first: SuperpairItem,
        second: SuperpairItem,
    ) = first.slotId != second.slotId && first.sameAs(second)

    // TODO extract logic greatly
    private fun hasFoundMatch(items: Map<Int, SuperpairItem>, firstItem: SuperpairItem) =
        items.any { it.value.slotId != firstItem.slotId && it.value.sameAs(firstItem) } &&
            found.entries.none {
                it.key.isAnyOf(FoundType.PAIR, FoundType.MATCH) &&
                    it.value.any { data ->
                        firstItem.slotId.equalsOneOf(data.first?.slotId ?: -1, data.second?.slotId ?: -1)
                    }
            }

    private fun isPowerUp(reward: String) = ExperimentationTableApi.powerUpPattern.matches(reward)

    private fun isReward(reward: String) = ExperimentationTableApi.rewardPattern.matches(reward) || isPowerUp(reward)

    private fun isWaiting(itemName: String) = waitingMessagesPattern.matches(itemName)

    private fun clicksSinceSeparator(list: MutableMap<Int, SuperpairItem>): Int {
        val lastIndex = list.entries.indexOfLast { it.value == emptySuperpairItem }
        return if (lastIndex != -1) list.size - 1 - lastIndex else -1
    }

    private fun isOutOfBounds(slot: Int, experiment: ExperimentTier): Boolean = slot !in experiment.slotRange

    private fun SuperpairItem?.sameAs(other: SuperpairItem) = this?.reward == other.reward && this.damage == other.damage
}
