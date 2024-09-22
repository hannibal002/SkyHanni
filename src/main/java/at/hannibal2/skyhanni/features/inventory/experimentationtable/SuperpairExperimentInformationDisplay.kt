package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent.SlotClickEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
// TODO important: all use cases of listOf in combination with string needs to be gone. no caching, constant new list creation, and bad design.
object SuperpairExperimentInformationDisplay {

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable

    data class Item(val index: Int, val item: ItemStack)
    data class ItemPair(val first: Item, val second: Item)
    data class FoundData(val item: Item = emptyItem, val pair: ItemPair = emptyPair)

    private val emptyItem = Item(-1, ItemStack(Blocks.air))
    private val emptyPair = ItemPair(Item(-1, ItemStack(Blocks.air)), Item(-1, ItemStack(Blocks.air)))

    private enum class FoundType {
        NORMAL,
        POWERUP,
        MATCH,
        PAIR
        ;

        fun isOneOf(vararg types: FoundType) = types.contains(this)
    }

    private var display = emptyList<String>()

    private var uncoveredAt = 0
    private var uncoveredItems = mutableListOf<Item>()
    private var possiblePairs = 0

    private var found = mutableMapOf<FoundData, FoundType>()

    private var toCheck = mutableListOf<Pair<Item, Int>>()
    private var lastClicked = mutableListOf<Pair<Int, Int>>()
    private var lastClick = SimpleTimeMark.farPast()
    private var currentExperiment = Experiment.NONE
    private var instantFind = 0

    private val sideSpaces1 = listOf(17, 18, 26, 27, 35, 36)
    private val sideSpaces2 = listOf(16, 17, 18, 19, 25, 26, 27, 28, 34, 35, 36, 37)

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        display = emptyList()

        uncoveredAt = 0
        uncoveredItems.clear()
        possiblePairs = 0

        found.clear()
        toCheck.clear()
        lastClicked.clear()
        lastClick = SimpleTimeMark.farPast()
        currentExperiment = Experiment.NONE
        instantFind = 0
    }

    @SubscribeEvent
    fun onChestGuiOverlayRendered(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.superpairDisplayPosition.renderStrings(display, posLabel = "Superpair Experiment Information")
        display = checkItems(toCheck)
    }

    @SubscribeEvent
    fun onSlotClick(event: SlotClickEvent) {
        if (!isEnabled()) return
        currentExperiment = ExperimentationTableAPI.getCurrentExperiment() ?: return

        if (isOutOfBounds(event.slotId, currentExperiment)) return
        val item = event.item ?: return
        if (item.displayName.removeColor() == "?") return
        val clicksItem = InventoryUtils.getItemAtSlotIndex(4)

        if (lastClicked.none { it.first == event.slotId && it.second == uncoveredAt } && lastClick.passedSince() > 100.milliseconds) {
            if (clicksItem != null && clicksItem.displayName.removeColor().split(" ")[1] == "0") return
            lastClicked.add(Pair(event.slotId, uncoveredAt))
            lastClick = SimpleTimeMark.now()
            DelayedRun.runDelayed(100.milliseconds) {
                toCheck.add(Item(event.slotId, item) to uncoveredAt)
                println(toCheck)
                uncoveredAt += 1
            }
        }
    }

    private fun checkItems(check: MutableList<Pair<(Item), Int>>): List<String> {
        currentExperiment = ExperimentationTableAPI.getCurrentExperiment() ?: return listOf()
        if (check.isEmpty()) return drawDisplay()

        for ((item, uncovered) in check) {
            val itemName = item.item.displayName.removeColor()

            if (isWaiting(itemName) || isOutOfBounds(item.index, currentExperiment)) return drawDisplay()

            val reward = convertToReward(item.item)
            if (uncoveredItems.none { it.index == item.index }) uncoveredItems.add(item)

            println(isPowerUp(reward) to isReward(itemName))

            when {
                isPowerUp(reward) -> handlePowerUp(item, reward)
                isReward(itemName) -> handleReward(item, uncovered)
            }

            possiblePairs = calculatePossiblePairs()

            val since = clicksSinceSeparator(lastClicked)

            if ((since >= 2 || (since == -1 && lastClicked.size >= 2)) && instantFind == 0) {
                lastClicked.add(-1 to uncoveredAt)
                uncoveredAt += 1
            }
            toCheck.removeIf { it.first == item }

            return drawDisplay()
        }
        possiblePairs = calculatePossiblePairs()
        return drawDisplay()
    }

    private fun handlePowerUp(item: Item, reward: String) {
        found[FoundData(item = item)] = FoundType.POWERUP
        println(found)
        possiblePairs--
        lastClicked.removeIf { it.first == item.index }
        uncoveredAt -= 1
        if (reward == "Instant Find") instantFind += 1
    }

    private fun handleReward(item: Item, uncovered: Int) {
        val lastSlotClicked =
            if (instantFind == 0 && lastClicked.none { it.first == -1 && it.second == uncovered - 1 } && lastClicked.size != 1) lastClicked.find { it.second == uncovered - 1 }
                ?: return else lastClicked.find { it.second == uncovered } ?: return

        val lastItem = InventoryUtils.getItemAtSlotIndex(lastSlotClicked.first) ?: return
        val lastItemName = convertToReward(lastItem)

        val pair = ItemPair(item, Item(lastSlotClicked.first, lastItem))

        if (isWaiting(lastItemName)) return

        println(hasFoundPair(pair) to hasFoundMatch(item))

        when {
            instantFind >= 1 -> {
                handleFoundPair(pair)
                instantFind -= 1
                lastClicked.add(-1 to uncoveredAt)
                uncoveredAt += 1
            }

            hasFoundPair(pair) -> handleFoundPair(pair)

            hasFoundMatch(item) -> handleFoundMatch(item)
            else -> handleNormalReward(item)
        }
    }

    private fun handleFoundPair(
        pair: ItemPair,
    ) {
        found[FoundData(pair = pair)] = FoundType.PAIR
        println(found)
        found.entries.removeIf {
            it.value == FoundType.MATCH && anyDuplicates(
                it.key.pair.first.index,
                it.key.pair.second.index,
                pair.first.index,
                pair.second.index,
            )
        }
        found.entries.removeIf {
            it.value == FoundType.NORMAL && anyDuplicates(it.key.item.index, pair.first.index, pair.second.index)
        }
        println(found)
    }

    private fun handleFoundMatch(item: Item) {
        val match = uncoveredItems.find { it.item == item.item } ?: return
        val pair = ItemPair(item, match)

        if (found.none {
                it.value.isOneOf(FoundType.MATCH, FoundType.PAIR) && anyDuplicates(
                    it.key.pair.first.index,
                    it.key.pair.second.index,
                    item.index,
                    match.index,
                )
            }) found[FoundData(pair = pair)] = FoundType.MATCH
        println(found)
        found.entries.removeIf { it.value == FoundType.NORMAL && anyDuplicates(it.key.item.index, item.index, match.index) }
    }

    private fun handleNormalReward(item: Item) {
        if (found.none {
                it.value.isOneOf(
                    FoundType.MATCH,
                    FoundType.PAIR,
                ) && anyDuplicates(it.key.pair.first.index, it.key.pair.second.index, item.index)
            } && found.none { it.value == FoundType.NORMAL && it.key.item.index == item.index }) found[FoundData(item = item)] =
            FoundType.NORMAL
        println(found)
    }

    private fun calculatePossiblePairs() =
        ((currentExperiment.gridSize - 2) / 2) - found.filter { it.value != FoundType.POWERUP }.size

    private fun drawDisplay() = buildList {
        add("§6Superpair Experimentation Data")
        add("")

        val normals = found.entries.filter { it.value == FoundType.NORMAL }
        val powerups = found.entries.filter { it.value == FoundType.POWERUP }
        val matches = found.entries.filter { it.value == FoundType.MATCH }
        val pairs = found.entries.filter { it.value == FoundType.PAIR }

        if (pairs.isNotEmpty()) add("§2Found")
        for (pair in pairs) {
            val prefix = determinePrefix(pairs.indexOf(pair), pairs.lastIndex)
            val reward = convertToReward(pair.key.pair.first.item)
            add(" $prefix §a$reward")
        }
        if (matches.isNotEmpty()) add("§eMatched")
        for (match in matches) {
            val prefix = determinePrefix(matches.indexOf(match), matches.lastIndex)
            val reward = convertToReward(match.key.pair.first.item)
            add(" $prefix §e$reward")
        }
        if (powerups.isNotEmpty()) add("§bPowerUp")
        for (powerup in powerups) {
            val prefix = determinePrefix(powerups.indexOf(powerup), powerups.size - 1)
            val reward = convertToReward(powerup.key.item.item)
            add(" $prefix §b$reward")
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

    private fun convertToReward(item: ItemStack) = if (item.displayName.removeColor() == "Enchanted Book") item.getLore()[2].removeColor()
    else item.displayName.removeColor()

    private fun determinePrefix(index: Int, lastIndex: Int) = if (index == lastIndex) "└" else "├"

    private fun hasFoundPair(
        pair: ItemPair,
    ) = pair.first.index != pair.second.index && pair.first.item == pair.second.item

    private fun hasFoundMatch(firstItem: Item) =
        uncoveredItems.any { (slot, item) ->
            slot != firstItem.index && item == firstItem.item
        } &&
            found.none {
                it.value.isOneOf(
                    FoundType.PAIR,
                    FoundType.MATCH,
                ) && anyDuplicates(it.key.pair.first.index, it.key.pair.second.index, firstItem.index)
            }

    private fun isPowerUp(reward: String) = ExperimentationTableAPI.powerUpPattern.matches(reward)

    private fun isReward(reward: String) = ExperimentationTableAPI.rewardPattern.matches(reward)

    private fun isWaiting(itemName: String) =
        listOf("Click any button!", "Click a second button!", "Next button is instantly rewarded!").contains(itemName)

    private fun clicksSinceSeparator(list: MutableList<Pair<Int, Int>>): Int {
        val lastIndex = list.indexOfLast { it.first == -1 }
        return if (lastIndex != -1) list.size - 1 - lastIndex else -1
    }

    private fun isOutOfBounds(slot: Int, experiment: Experiment): Boolean =
        slot <= experiment.startSlot || slot >= experiment.endSlot || (if (experiment.sideSpace == 1) slot in sideSpaces1 else slot in sideSpaces2)

    private fun anyDuplicates(vararg slots: Int) = slots.any { slot -> slots.filter { it == slot }.size >= 2 }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.superpairDisplay && ExperimentationTableAPI.getCurrentExperiment() != null
}
