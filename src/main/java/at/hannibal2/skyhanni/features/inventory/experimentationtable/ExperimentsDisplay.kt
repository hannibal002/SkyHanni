package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.inventory.ExperimentationTableConfig.Experiments
import at.hannibal2.skyhanni.events.GuiContainerEvent.SlotClickEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object ExperimentsDisplay {

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable

    private var display = emptyList<String>()

    private var uncoveredAt = 0
    private var uncoveredItems = mutableListOf<Pair<Int, String>>()
    private var possiblePairs = 0

    data class ItemPair(val first: Pair<Int, String>, val second: Pair<Int, String>)

    private var foundNormals = mutableMapOf<Int, String>()
    private var foundMatches = mutableListOf<ItemPair>()
    private var foundPairs = mutableListOf<ItemPair>()
    private var foundPowerUps = mutableMapOf<Int, String>()
    private var toCheck = mutableListOf<Pair<Int, Int>>()
    private var lastClicked = mutableListOf<Pair<Int, Int>>()
    private var lastClick = SimpleTimeMark.farPast()
    private var currentExperiment = Experiments.NONE
    private var instantFind = false

    private val patternGroup = RepoPattern.group("enchanting.experiments")
    private val powerUpPattern by patternGroup.pattern(
        "powerups",
        "Gained \\+\\d Clicks?|Instant Find|\\+\\S* XP",
    )

    private val rewardPattern by patternGroup.pattern(
        "rewards",
        "\\d{1,3}k Enchanting Exp|Enchanted Book|(?:Titanic |Grand |\\b)Experience Bottle|Metaphysical Serum|Experiment The Fish",
    )

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        display = emptyList()

        uncoveredAt = 0
        uncoveredItems.clear()
        possiblePairs = 0

        foundNormals.clear()
        foundMatches.clear()
        foundPairs.clear()
        foundPowerUps.clear()
        toCheck.clear()
        lastClicked.clear()
        lastClick = SimpleTimeMark.farPast()
        currentExperiment = Experiments.NONE
        instantFind = false
    }

    @SubscribeEvent
    fun onChestGuiOverlayRendered(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.informationDisplayPosition.renderStrings(
            display,
            posLabel = "Experiment Information Display",
        )
        display = checkItems(toCheck)
    }

    @SubscribeEvent
    fun onSlotClick(event: SlotClickEvent) {
        if (!isEnabled()) return
        currentExperiment = InventoryUtils.getCurrentExperiment() ?: return

        if (isOutOfBounds(event.slotId, currentExperiment)) return
        val item = event.item ?: return
        if (item.displayName.removeColor() == "?") return

        if (lastClicked.none { it.first == event.slotId && it.second == uncoveredAt } && lastClick.passedSince() > 100.milliseconds) {
            lastClicked.add(Pair(event.slotId, uncoveredAt))
            lastClick = SimpleTimeMark.now()
            toCheck.add(event.slotId to uncoveredAt)
            uncoveredAt += 1
        }
    }

    private fun checkItems(check: MutableList<Pair<Int, Int>>): List<String> {
        currentExperiment = InventoryUtils.getCurrentExperiment() ?: return listOf()
        if (check.isEmpty()) return drawDisplay()

        check.forEachIndexed { index, (slot, uncovered) ->
            val itemNow = InventoryUtils.getItemAtSlotIndex(slot) ?: return drawDisplay()
            val itemName = itemNow.displayName.removeColor()

            if (isWaiting(itemName) || isOutOfBounds(slot, currentExperiment))
                return drawDisplay()

            val reward = convertToReward(itemNow)
            if (uncoveredItems.none { it.first == slot }) uncoveredItems.add(Pair(slot, reward))

            when {
                isPowerUp(reward) -> handlePowerUp(slot, reward)
                isReward(itemName) -> handleReward(index, slot, uncovered, reward)
            }

            possiblePairs = calculatePossiblePairs()

            val since = clicksSinceSeparator(lastClicked)

            if ((since >= 2 || (since == -1 && lastClicked.size >= 2)) && !instantFind) {
                lastClicked.add(-1 to uncoveredAt)
                uncoveredAt += 1
            }
            toCheck.removeIf { it.first == slot }

            return drawDisplay()
        }
        possiblePairs = calculatePossiblePairs()
        return drawDisplay()
    }

    private fun handlePowerUp(slot: Int, reward: String) {
        foundPowerUps[slot] = reward
        possiblePairs--
        lastClicked.removeIf { it.first == slot }
        uncoveredAt -= 1
        if (reward == "Instant Find") instantFind = true
    }

    private fun handleReward(index: Int, slot: Int, uncovered: Int, reward: String) {
        val lastSlotClicked =
            if (!instantFind && lastClicked.none { it.first == -1 && it.second == uncovered - 1 } && lastClicked.size != 1)
                lastClicked.find { it.second == uncovered - 1 } ?: return else lastClicked.find { it.second == uncovered } ?: return

        lastSlotClicked.let {
            val lastItem = InventoryUtils.getItemAtSlotIndex(it.first) ?: return
            val lastItemName = convertToReward(lastItem)

            if (isWaiting(lastItemName)) return

            when {
                instantFind -> {
                    handleFoundPair(slot, reward, it.first, lastItemName)
                    instantFind = false
                    lastClicked.add(-1 to uncoveredAt)
                    uncoveredAt += 1
                }

                hasFoundPair(slot, it.first, reward, lastItemName) -> handleFoundPair(
                    slot,
                    reward,
                    it.first,
                    lastItemName,
                )

                hasFoundMatch(slot, reward) -> handleFoundMatch(slot, reward)
                else -> handleNormalReward(slot, reward)
            }
        }
    }

    private fun handleFoundPair(
        slot: Int,
        reward: String,
        lastSlotClicked: Int,
        lastItemName: String,
    ) {
        foundPairs.add(ItemPair(Pair(slot, reward), Pair(lastSlotClicked, lastItemName)))
        foundMatches.removeAll { it.first.second == reward }
        foundNormals.entries.removeIf { it.value == reward }
    }

    private fun handleFoundMatch(slot: Int, reward: String) {
        val match = uncoveredItems.find { it.second == reward }?.first ?: return
        foundMatches.add(ItemPair(Pair(slot, reward), Pair(match, reward)))
        foundNormals.entries.removeIf { it.value == reward }
    }

    private fun handleNormalReward(slot: Int, reward: String) {
        if (foundMatches.none { it.first.second == reward } && foundPairs.none { it.first.second == reward }) foundNormals[slot] = reward
    }

    private fun calculatePossiblePairs() =
        ((currentExperiment.gridSize - 2) / 2) - foundPairs.size - foundMatches.size - foundNormals.size

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
        val toAdd = mutableListOf<String>()
        if (possiblePairs >= 1) toAdd.add("§ePairs - ${possiblePairs}")
        if (2 - foundPowerUps.size >= 1) toAdd.add("§bPowerUps - ${2 - foundPowerUps.size}")
        if (foundNormals.isNotEmpty()) toAdd.add("§7Normals - ${foundNormals.size}")

        if (toAdd.isNotEmpty()) {
            add("")
            add("§4Not found")
        }
        for (string in toAdd) if (string != toAdd.last()) add(" ├ $string") else add(" └ $string")
    }

    private fun convertToReward(item: ItemStack) =
        if (item.displayName.removeColor() == "Enchanted Book") item.getLore()[2].removeColor()
        else item.displayName.removeColor()

    private fun determinePrefix(index: Int, lastIndex: Int) = if (index == lastIndex) "└" else "├"

    private fun hasFoundPair(
        firstSlot: Int,
        secondSlot: Int,
        firstName: String,
        secondName: String,
    ) = firstSlot != secondSlot && firstName == secondName

    private fun hasFoundMatch(itemSlot: Int, reward: String) =
        uncoveredItems.any { (slot, name) -> slot != itemSlot && name == reward } &&
            foundMatches.none { it.first.second == reward } &&
            foundPairs.none { it.first.second == reward }

    private fun isPowerUp(reward: String) = powerUpPattern.matches(reward)

    private fun isReward(reward: String) = rewardPattern.matches(reward)

    private fun isWaiting(itemName: String) =
        listOf("Click any button!", "Click a second button!", "Next button is instantly rewarded!")
            .contains(itemName)

    private fun clicksSinceSeparator(list: MutableList<Pair<Int, Int>>): Int {
        val lastIndex = list.indexOfLast { it.first == -1 }
        return if (lastIndex != -1) list.size - 1 - lastIndex else -1
    }

    private fun isOutOfBounds(slot: Int, experiment: Experiments) =
        slot <= experiment.startSlot || slot >= experiment.startSlot + experiment.gridSize + 7 ||
            listOf(17, 18, 26, 27, 35, 36).contains(slot)

    private fun isEnabled() =
        config.experimentationTableDisplay && LorenzUtils.inSkyBlock && InventoryUtils.openInventoryName().startsWith("Superpairs (")
}
