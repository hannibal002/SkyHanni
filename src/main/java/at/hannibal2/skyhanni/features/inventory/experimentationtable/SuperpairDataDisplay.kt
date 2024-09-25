package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent.SlotClickEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
// TODO important: all use cases of listOf in combination with string needs to be gone. no caching, constant new list creation, and bad design.
object SuperpairDataDisplay {

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable

    data class Item(val index: Int, val reward: String, val damage: Int)
    data class ItemPair(val first: Item, val second: Item)
    data class FoundData(val item: Item = emptyItem, val pair: ItemPair = emptyPair)

    private val emptyItem = Item(-1, "", -1)
    private val emptyPair = ItemPair(emptyItem, emptyItem)

    private enum class FoundType {
        NORMAL,
        POWERUP,
        MATCH,
        PAIR
        ;

        fun isOneOf(vararg types: FoundType) = types.contains(this)
    }

    private val sideSpaces1 = listOf(17, 18, 26, 27, 35, 36)
    private val sideSpaces2 = listOf(16, 17, 18, 19, 25, 26, 27, 28, 34, 35, 36, 37)

    private var display = emptyList<String>()
    private var uncoveredItems = mutableMapOf<Int, Item>()
    private var found = mutableMapOf<FoundData, FoundType>()

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        display = emptyList()

        uncoveredItems = mutableMapOf()
        found.clear()
    }

    @SubscribeEvent
    fun onChestGuiOverlayRendered(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (display.isEmpty()) display = drawDisplay()

        config.superpairDisplayPosition.renderStrings(display, posLabel = "Superpair Experiment Information")
    }

    @SubscribeEvent
    fun onSlotClick(event: SlotClickEvent) {
        if (!isEnabled()) return
        val currentExperiment = ExperimentationTableAPI.getCurrentExperiment() ?: return

        val item = event.item ?: return
        if (isOutOfBounds(event.slotId, currentExperiment) || item.displayName.removeColor() == "?") return

        val clicksItem = InventoryUtils.getItemAtSlotIndex(4)

        if (uncoveredItems.none { it.value.index == event.slotId && it.key == uncoveredItems.keys.max() }) {
            if (clicksItem != null && clicksItem.displayName.removeColor().split(" ")[1] == "0") return

            DelayedRun.runDelayed(200.milliseconds) {
                val itemNow = InventoryUtils.getItemAtSlotIndex(event.slotId) ?: return@runDelayed
                val itemName = itemNow.displayName.removeColor()
                val reward = convertToReward(itemNow)
                val itemData = Item(event.slotId, reward, itemNow.itemDamage)
                val uncovered = uncoveredItems.keys.maxOrNull() ?: -1

                if (isWaiting(itemName)) return@runDelayed

                if (uncoveredItems.none { it.key == uncovered && it.value.index == event.slotId })
                    uncoveredItems[uncovered + 1] = itemData

                when {
                    isPowerUp(reward) -> handlePowerUp(itemData, uncovered + 1)
                    isReward(itemName) -> handleReward(itemData, uncovered + 1)
                }

                val since = clicksSinceSeparator(uncoveredItems)

                if ((since >= 2 || (since == -1 && uncoveredItems.size >= 2)))
                    uncoveredItems[uncovered + 2] = emptyItem

                display = drawDisplay()
            }
        }
    }

    private fun handlePowerUp(item: Item, uncovered: Int) {
        if (item.reward != "Instant Find") uncoveredItems.remove(uncovered)

        found[FoundData(item = item)] = FoundType.POWERUP
    }

    private fun handleReward(item: Item, uncovered: Int) {
        val last = uncoveredItems.getOrDefault(uncovered - 1, item)

        val pair = ItemPair(item, last)

        if (isWaiting(last.reward)) return

        when {
            last.reward == "Instant Find" -> handleInstantFind(item, uncovered)
            hasFoundPair(pair) -> handleFoundPair(pair)
            hasFoundMatch(item) -> handleFoundMatch(item)
            else -> handleNormalReward(item)
        }
    }

    private fun handleInstantFind(item: Item, uncovered: Int) {
        uncoveredItems[uncovered - 1] = item
        uncoveredItems[uncovered] = emptyItem

        handleFoundPair(ItemPair(item, emptyItem))
    }

    private fun handleFoundPair(
        pair: ItemPair,
    ) {
        found.entries.removeIf {
            when (it.value) {
                FoundType.MATCH -> it.key.pair.first.sameAs(pair.first)
                FoundType.NORMAL -> it.key.item.sameAs(pair.first) || it.key.item.sameAs(pair.second)
                else -> false
            }
        }

        found[FoundData(pair = pair)] = FoundType.PAIR
    }

    private fun handleFoundMatch(item: Item) {
        val match = uncoveredItems.values.find { it.index != item.index && it.sameAs(item) } ?: return
        val pair = ItemPair(item, match)

        if (found.any {
                it.value.isOneOf(FoundType.MATCH, FoundType.PAIR) && (
                    anyDuplicates(it.key.pair.first.index, item.index, match.index) ||
                        anyDuplicates(it.key.pair.first.index, item.index, match.index))
            }) return

        found.entries.removeIf { it.value == FoundType.NORMAL && anyDuplicates(it.key.item.index, item.index, match.index) }
        found[FoundData(pair = pair)] = FoundType.MATCH
    }

    private fun handleNormalReward(item: Item) {
        if (found.any {
                when {
                    it.value.isOneOf(FoundType.MATCH, FoundType.PAIR) ->
                        anyDuplicates(item.index, it.key.pair.first.index, it.key.pair.second.index)

                    else -> it.key.item.index == item.index && it.key.item.sameAs(item)
                }
            }) return

        found[FoundData(item = item)] = FoundType.NORMAL
    }

    private fun drawDisplay() = buildList {
        val currentExperiment = ExperimentationTableAPI.getCurrentExperiment() ?: return emptyList<String>()

        add("§6Superpair Experimentation Data")
        add("")

        val normals = found.entries.filter { it.value == FoundType.NORMAL }
        val powerups = found.entries.filter { it.value == FoundType.POWERUP }
        val matches = found.entries.filter { it.value == FoundType.MATCH }
        val pairs = found.entries.filter { it.value == FoundType.PAIR }
        val possiblePairs = calculatePossiblePairs(currentExperiment)

        if (pairs.isNotEmpty()) add("§2Found")
        for (pair in pairs) {
            val prefix = determinePrefix(pairs.indexOf(pair), pairs.lastIndex)
            add(" $prefix §a${pair.key.pair.first.reward}")
        }
        if (matches.isNotEmpty()) add("§eMatched")
        for (match in matches) {
            val prefix = determinePrefix(matches.indexOf(match), matches.lastIndex)
            add(" $prefix §e${match.key.pair.first.reward}")
        }
        if (powerups.isNotEmpty()) add("§bPowerUp")
        for (powerup in powerups) {
            val prefix = determinePrefix(powerups.indexOf(powerup), powerups.size - 1)
            add(" $prefix §b${powerup.key.item.reward}")
        }
        val toAdd = mutableListOf<String>()
        if (possiblePairs >= 1) toAdd.add("§ePairs - $possiblePairs")
        if (2 - powerups.size >= 1) toAdd.add("§bPowerUps - ${2 - powerups.size}")
        if (normals.isNotEmpty()) toAdd.add("§7Normals - ${normals.size}")

        if (toAdd.isNotEmpty()) {
            add("")
            add("§4Not found")
        }
        for (string in toAdd) if (string != toAdd.last()) add(" ├ $string") else add(" └ $string")
    }

    private fun calculatePossiblePairs(currentExperiment: Experiment) =
        ((currentExperiment.gridSize - 2) / 2) - found.filter { it.value != FoundType.POWERUP }.size

    private fun convertToReward(item: ItemStack) = if (item.displayName.removeColor() == "Enchanted Book") item.getLore()[2].removeColor()
    else item.displayName.removeColor()

    private fun determinePrefix(index: Int, lastIndex: Int) = if (index == lastIndex) "└" else "├"

    private fun hasFoundPair(
        pair: ItemPair,
    ) = pair.first.index != pair.second.index && pair.first.sameAs(pair.second)

    private fun hasFoundMatch(firstItem: Item) =
        uncoveredItems.any { it.value.index != firstItem.index && it.value.sameAs(firstItem) } &&
            found.none {
                it.value.isOneOf(
                    FoundType.PAIR,
                    FoundType.MATCH,
                ) && anyDuplicates(firstItem.index, it.key.pair.first.index, it.key.pair.second.index)
            }

    private fun isPowerUp(reward: String) = ExperimentationTableAPI.powerUpPattern.matches(reward)

    private fun isReward(reward: String) =
        ExperimentationTableAPI.rewardPattern.matches(reward) || ExperimentationTableAPI.powerUpPattern.matches(reward)

    private fun isWaiting(itemName: String) =
        listOf("Click any button!", "Click a second button!", "Next button is instantly rewarded!").contains(itemName)

    private fun clicksSinceSeparator(list: MutableMap<Int, Item>): Int {
        val lastIndex = list.entries.indexOfLast { it.value == emptyItem }
        return if (lastIndex != -1) list.size - 1 - lastIndex else -1
    }

    private fun isOutOfBounds(slot: Int, experiment: Experiment): Boolean =
        slot <= experiment.startSlot || slot >= experiment.endSlot ||
            (if (experiment.sideSpace == 1) slot in sideSpaces1 else slot in sideSpaces2)

    private fun anyDuplicates(search: Any, vararg toSearch: Any) = search in toSearch

    private fun Item.sameAs(other: Item) =
        this.reward == other.reward && this.damage == other.damage

    private fun isEnabled() =
        config.superpairDisplay && ExperimentationTableAPI.getCurrentExperiment() != null
}
