package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.inventory.helper.HelperConfig.Experiments
import at.hannibal2.skyhanni.events.GuiContainerEvent.BackgroundDrawnEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent.SlotClickEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Matcher

@SkyHanniModule
object ExperimentationTableDisplay {

    private val config get() = SkyHanniMod.feature.inventory.helper.enchanting
    private var display = emptyList<String>()
    private val startSlot = 9

    private var skip = false

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
        "Superpairs \\((?<experiment>.+)\\)"
    )
    private val powerUpPattern by patternGroup.pattern(
        "powerups",
        "Gained \\+\\d Clicks?|Instant Find|\\+\\S* XP"
    )
    private val rewardPattern by patternGroup.pattern(
        "rewards",
        "\\d{1,3}k Enchanting Exp|Enchanted Book|(?:Titanic |Grand |\\b)Experience Bottle|Metaphysical Serum|Experiment The Fish"
    )

    @SubscribeEvent
    fun onBackgroundDrawn(event: BackgroundDrawnEvent) {
        if (!isEnabled()) return
        config.informationDisplayPosition.renderStrings(display, posLabel = "Experiment Information Display")
        superpairsPattern.matchMatcher(
            InventoryUtils.openInventoryName(),
            fun Matcher.() {
                currentExperiment = Experiments.entries.find { it.name == group("experiment") } ?: return

                for (item in toCheck) {
                    val slot = item.first
                    val items = InventoryUtils.getItemsInOpenChest()
                    val itemNow = items[slot].stack
                    val itemName = itemNow.displayName.removeColor()

                    if (itemName == "Click any button!") break
                    if (isOutOfBounds(slot, currentExperiment)) return

                    val reward = convertToReward(itemNow)

                    if (!uncoveredItems.containsKey(slot)) uncoveredItems[slot] = reward

                    if (isPowerUp(reward)) {
                        foundPowerUps[slot] = reward
                        possiblePairs--
                        lastClicked.clear()
                    } else if (isReward(itemName)) {
                        skip = false

                        val lastSlotClicked = lastClicked[toCheck.indexOf(item) - 1]
                        val lastItemName = items[lastSlotClicked].stack.displayName
                        val foundPair = hasFoundPair(slot, lastSlotClicked, reward, lastItemName)
                        val foundMatch = hasFoundMatch(reward, slot)

                        if (foundPair || foundMatch) foundNormals.entries.removeIf { it.value == reward }
                        if (foundPairs.any { it.first.second == reward }) skip = true

                        when {
                            foundPair && !skip -> {
                                foundPairs.add(ItemPair(
                                        Pair(slot, reward), Pair(lastSlotClicked, lastItemName))
                                )
                                foundMatches.removeAll { it.first.second == reward }
                                lastClicked.clear()
                                skip = true
                            }

                            foundMatch && !skip -> {
                                val match = uncoveredItems.entries.find { it.value == reward }?.key ?: return
                                foundMatches.add(ItemPair(
                                        Pair(slot, reward), Pair(match, reward))
                                )
                                skip = true
                            }
                        }

                        if (!skip && foundMatches.none { it.first.second == reward }) {
                            foundNormals[slot] = reward
                            possiblePairs--
                        }
                    }

                    lastClicked.removeAt(toCheck.indexOf(item) - 1)
                    toCheck.remove(item)

                    possiblePairs =
                        (currentExperiment.gridSize / 2) - foundPairs.size - foundPowerUps.size - foundMatches.size

                    display = drawDisplay()
                }
            }
        )
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        foundNormals = mutableMapOf()
        foundPairs = mutableListOf()
        uncoveredItems = mutableMapOf()
        foundPowerUps = mutableMapOf()
        foundMatches = mutableListOf()
        toCheck = mutableListOf()
        lastClicked = mutableListOf()
        display = emptyList()
    }

    @SubscribeEvent
    fun onSlotClick(event: SlotClickEvent) {
        if (!isEnabled()) return
        superpairsPattern.matchMatcher(InventoryUtils.openInventoryName()) {
            currentExperiment = Experiments.entries.find { it.name == group("experiment") } ?: return

            val item = event.item ?: return
            if (isOutOfBounds(event.slotId, currentExperiment)) return

            toCheck.add(Pair(event.slotId, item))
            lastClicked.add(event.slotId)
        }
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
            val prefix = determinePrefix(foundPowerUps.entries.indexOf(power), foundPowerUps.size - 1)
            add(" $prefix §b${power.value}")
        }
        add("")
        add("§4Not found")
        add(" ├ §ePairs - $possiblePairs")
        add(" └ §7Normals - ${foundNormals.size}")
    }

    private fun convertToReward(item: ItemStack) =
        if (item.displayName.removeColor() == "Enchanted Book") item.getLore()[2].removeColor() else item.displayName

    private fun determinePrefix(index: Int, lastIndex: Int) =
        if (index == lastIndex) "└" else "├"

    private fun hasFoundPair(firstSlot: Int, secondSlot: Int, firstName: String, secondName: String) =
        firstSlot != secondSlot && firstName == secondName

    private fun hasFoundMatch(reward: String, itemSlot: Int) =
        uncoveredItems.any { (slot, name) -> slot != itemSlot && name == reward } && foundMatches.none { it.first.second == reward }

    private fun isPowerUp(reward: String) =
        powerUpPattern.matches(reward)

    private fun isReward(reward: String) =
        rewardPattern.matches(reward)

    private fun isOutOfBounds(slot: Int, experiment: Experiments) =
        slot <= startSlot || slot >= startSlot + experiment.gridSize + 6

    private fun isEnabled() =
        config.experimentationTableDisplay && InventoryUtils.openInventoryName().startsWith("Superpairs (")

}
