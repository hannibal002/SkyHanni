package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.inventory.helper.HelperConfig.Experiments
import at.hannibal2.skyhanni.events.GuiContainerEvent.SlotClickEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object ExperimentationTableDisplay {

    private val config get() = SkyHanniMod.feature.inventory.helper.enchanting

    private var display = emptyList<String>()
    private val startSlot = 9

    private var uncoveredItems = mutableMapOf<Int, String>()
    private var possiblePairs = 0

    data class ItemPair(val first: Pair<Int, String>, val second: Pair<Int, String>)

    private var foundNormals = mutableMapOf<Int, String>()
    private var foundMatches = mutableListOf<ItemPair>()
    private var foundPairs = mutableListOf<ItemPair>()
    private var foundPowerUps = mutableMapOf<Int, String>()
    private var toCheck = mutableListOf<Pair<Int, ItemStack>>()
    private var lastClicked = mutableListOf<Int>()
    private var currentExperiment = Experiments.NONE

    private val patternGroup = RepoPattern.group("enchanting.experiments")
    private val superpairsPattern by patternGroup.pattern(
        "superpairs",
        "Superpairs \\((?<experiment>.+)\\)")

    private val powerUpPattern by patternGroup.pattern(
        "powerups",
        "Gained \\+\\d Clicks?|Instant Find|\\+\\S* XP")

    private val rewardPattern by patternGroup.pattern(
        "rewards",
        "\\d{1,3}k Enchanting Exp|Enchanted Book|(?:Titanic |Grand |\\b)Experience Bottle|Metaphysical Serum|Experiment The Fish",
    )

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        foundNormals.clear()
        foundPairs.clear()
        uncoveredItems.clear()
        foundPowerUps.clear()
        foundMatches.clear()
        toCheck.clear()
        lastClicked.clear()
        display = emptyList()
    }

    @SubscribeEvent
    fun onChestGuiOverlayRendered(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.informationDisplayPosition.renderStrings(
            display,
            posLabel = "Experiment Information Display"
        )
        display = checkItems(toCheck)
    }

    @SubscribeEvent
    fun onSlotClick(event: SlotClickEvent) {
        if (!isEnabled()) return
        superpairsPattern.matchMatcher(InventoryUtils.openInventoryName()) {
            currentExperiment =
                Experiments.entries.find { it.name == group("experiment") } ?: return

            val item = event.item ?: return
            if (isOutOfBounds(event.slotId, currentExperiment)) return

            if (toCheck.none { it.first == event.slotId && it.second == item }) {
                toCheck.add(Pair(event.slotId, item))
            }
            lastClicked.add(event.slotId)
        }
    }

    private fun checkItems(check: MutableList<Pair<Int, ItemStack>>): List<String> =
        superpairsPattern.matchMatcher(InventoryUtils.openInventoryName()) {
            currentExperiment =
                Experiments.entries.find { it.name == group("experiment") } ?: return listOf()
            if (check.isEmpty()) return drawDisplay()

            check.forEachIndexed { index, (slot, _) ->
                val itemNow = InventoryUtils.getItemAtSlotIndex(slot) ?: return drawDisplay()
                val itemName = itemNow.displayName.removeColor()

                if (isWaiting(itemName) || isOutOfBounds(slot, currentExperiment))
                    return drawDisplay()

                val reward = convertToReward(itemNow)
                uncoveredItems.putIfAbsent(slot, reward)

                when {
                    isPowerUp(reward) -> handlePowerUp(slot, reward)
                    isReward(itemName) -> handleReward(index, slot, reward, itemName)
                }

                possiblePairs = calculatePossiblePairs()

                check.removeAt(index)
                removeAtOrNull(index - 1, lastClicked) ?: return drawDisplay()

                return drawDisplay()
            }
            possiblePairs = calculatePossiblePairs()
            return drawDisplay()
        } ?: listOf()

    private fun handlePowerUp(slot: Int, reward: String) {
        foundPowerUps[slot] = reward
        possiblePairs--
        lastClicked.clear()
        if (reward == "Instant Find") lastClicked.add(slot)
    }

    private fun handleReward(index: Int, slot: Int, reward: String, itemName: String) {
        val lastSlotClicked =
            lastClicked.getOrNull(lastClicked.size - 2) ?: lastClicked.getOrNull(index)

        lastSlotClicked?.let {
            val lastItem = InventoryUtils.getItemAtSlotIndex(it) ?: return
            val lastItemName = convertToReward(lastItem)

            if (isWaiting(lastItemName)) return

            when {
                lastItemName == "Instant Find" -> handleFoundPair(slot, reward, it, lastItemName)
                hasFoundPair(slot, it, reward, lastItemName) -> handleFoundPair(slot, reward, it, lastItemName)
                hasFoundMatch(reward, slot) -> handleFoundMatch(slot, reward)
                else -> handleNormalReward(slot, reward)
            }
        }
    }

    private fun handleFoundPair(
        slot: Int,
        reward: String,
        lastSlotClicked: Int,
        lastItemName: String
    ) {
        foundPairs.add(ItemPair(Pair(slot, reward), Pair(lastSlotClicked, lastItemName)))
        foundMatches.removeAll { it.first.second == reward }
        lastClicked.removeIf { lastClicked.indexOf(it) <= lastClicked.indexOf(slot) }
        foundNormals.entries.removeIf { it.value == reward }
    }

    private fun handleFoundMatch(slot: Int, reward: String) {
        val match = uncoveredItems.entries.find { it.value == reward }?.key ?: return
        foundMatches.add(ItemPair(Pair(slot, reward), Pair(match, reward)))
        foundNormals.entries.removeIf { it.value == reward }
    }

    private fun handleNormalReward(slot: Int, reward: String) {
        if (foundMatches.none { it.first.second == reward }) {
            foundNormals[slot] = reward
            possiblePairs--
        }
    }

    private fun calculatePossiblePairs() =
        (currentExperiment.gridSize / 2) - foundPairs.size - foundPowerUps.size - foundMatches.size

    private fun removeAtOrNull(index: Int, collection: MutableList<Int>): Int? {
        return if (index in collection.indices) collection.removeAt(index) else null
    }

    private fun drawDisplay() = buildList {
        add("§6Experimentation Data")
        add("")
        if (foundPairs.isNotEmpty()) add("§2Found")
        for (pair in foundPairs) {
            val prefix = determinePrefix(foundPairs.indexOf(pair), foundPairs.lastIndex)
            add(" $prefix §a${pair.first.second}")
        }
        if (foundMatches.isNotEmpty()) add("§eMatched")
        for (pair in foundMatches) {
            val prefix = determinePrefix(foundMatches.indexOf(pair), foundMatches.lastIndex)
            add(" $prefix §e${pair.first.second}")
        }
        if (foundPowerUps.isNotEmpty()) add("§bPowerUp")
        for (power in foundPowerUps) {
            val prefix =
                determinePrefix(foundPowerUps.entries.indexOf(power), foundPowerUps.size - 1)
            add(" $prefix §b${power.value}")
        }
        add("")
        add("§4Not found")
        add(" ├ §ePairs - $possiblePairs")
        add(" └ §7Normals - ${foundNormals.size}")
    }

    private fun convertToReward(item: ItemStack) =
        if (item.displayName.removeColor() == "Enchanted Book") item.getLore()[2].removeColor()
        else item.displayName.removeColor()

    private fun determinePrefix(index: Int, lastIndex: Int) = if (index == lastIndex) "└" else "├"

    private fun hasFoundPair(
        firstSlot: Int,
        secondSlot: Int,
        firstName: String,
        secondName: String
    ) = firstSlot != secondSlot && firstName == secondName

    private fun hasFoundMatch(reward: String, itemSlot: Int) =
        uncoveredItems.any { (slot, name) -> slot != itemSlot && name == reward } &&
            foundMatches.none { it.first.second == reward }

    private fun isPowerUp(reward: String) = powerUpPattern.matches(reward)

    private fun isReward(reward: String) = rewardPattern.matches(reward)

    private fun isWaiting(itemName: String) =
        listOf("Click any button!", "Click a second button!", "Next button is instantly rewarded!")
            .contains(itemName)

    private fun isOutOfBounds(slot: Int, experiment: Experiments) =
        slot <= startSlot || slot >= startSlot + experiment.gridSize + 6

    private fun isEnabled() =
        LorenzUtils.inSkyBlock && config.experimentationTableDisplay &&
            InventoryUtils.openInventoryName().startsWith("Superpairs (")
}
