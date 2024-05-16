package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.inventory.ExperimentationTableDisplayConfig.Experiments
import at.hannibal2.skyhanni.events.GuiContainerEvent.BackgroundDrawnEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent.SlotClickEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ExperimentationTableDisplay {

    private val config get() = SkyHanniMod.feature.inventory.experimentationTableDisplay
    private var display = emptyList<String>()
    private val startSlot = 9

    private var skip = false

    private var uncoveredItems = mutableMapOf<Int, String>()
    private var possiblePairs = 0
    private var possiblePowerUps = 0

    data class ItemPair(val first: Pair<Int, String>, val second: Pair<Int, String>)

    private var foundNormals = mutableMapOf<Int, String>()
    private var foundMatches = mutableListOf<ItemPair>()
    private var foundPairs = mutableListOf<ItemPair>()
    private var foundPowerUps = mutableMapOf<Int, String>()
    private var toCheck = mutableMapOf<Int, ItemStack>()
    private var lastClicked = mutableMapOf<Int, String>()
    private var currentExperiment = Experiments.NONE

    private val patternGroup = RepoPattern.group("enchanting.experiments")
    private val superpairsPattern by patternGroup.pattern(
        "superpairs",
        "Superpairs \\((?<experiment>.+)\\)"
    )
    private val rewardsPattern by patternGroup.pattern(
        "rewards",
        "(?<reward>Enchanting Exp|Clicks|Enchanted Book|Instant Find|Bottle|Click)"
    )

    @SubscribeEvent
    fun onBackgroundDrawn(event: BackgroundDrawnEvent) {
        if (!isEnabled()) return
        config.informationDisplayPosition.renderStrings(display, posLabel = "Experiment Information Display")
        superpairsPattern.matchMatcher(InventoryUtils.openInventoryName()) {
            currentExperiment = Experiments.entries.find { it.name == group("experiment") } ?: return

            for (item in toCheck) {
                val slot = item.key
                val itemName = InventoryUtils.getItemsInOpenChest()[slot].stack.displayName.removeColor()

                if (isOutOfBounds(slot, currentExperiment)) return

                rewardsPattern.matchMatcher(itemName) {
                    val reward = group("reward") ?: return
                    skip = false

                    if (!uncoveredItems.containsKey(slot)) uncoveredItems[slot] = itemName

                    if (isPowerUp(reward)) {
                        if (!foundPowerUps.containsValue(itemName)) {
                            foundPowerUps[slot] = itemName
                            possiblePairs--
                        }
                    } else {
                        val lastLastClicked = lastClicked.entries.first()
                        val lastSlotClicked = lastLastClicked.key
                        val lastItemName = lastLastClicked.value

                        val foundPair = hasFoundPair(slot, lastSlotClicked, itemName, lastItemName)
                        println(listOf(slot, lastSlotClicked, itemName, lastItemName))
                        val foundMatch = hasFoundMatch(itemName, slot)

                        if (foundPair || foundMatch) foundNormals.entries.removeIf { it.value == itemName }

                        if (foundPairs.any { it.first.second == itemName }) skip = true

                        when {
                            foundPair && !skip -> {
                                foundPairs.add(
                                    ItemPair(
                                        Pair(slot, itemName),
                                        Pair(lastSlotClicked, lastItemName)
                                    )
                                )
                                if (foundMatches.any { it.first.second == itemName }) foundMatches.removeAll { it.first.second == itemName }
                                skip = true
                            }
                            foundMatch && !skip -> {
                                val match = uncoveredItems.entries.find { it.value == itemName }?.key ?: return
                                foundMatches.add(
                                    ItemPair(
                                        Pair(slot, itemName),
                                        Pair(match, itemName)
                                    )
                                )
                                skip = true
                            }
                        }
                    }

                    if (!isPowerUp(reward) && !skip) {
                        foundNormals[slot] = itemName
                        possiblePairs--
                    }

                    if (lastClicked.size == 2) lastClicked.remove(slot)
                    toCheck.remove(slot)
                }

                possiblePairs = (currentExperiment.gridSize / 2) - foundPairs.size - foundPowerUps.size - foundMatches.size

                display = drawDisplay()
            }
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        println(uncoveredItems)
        println(possiblePairs)
        println(foundPairs)
        println(foundMatches)
        println(foundPowerUps)
        foundNormals = mutableMapOf()
        foundPairs = mutableListOf()
        uncoveredItems = mutableMapOf()
        foundPowerUps = mutableMapOf()
        foundMatches = mutableListOf()
        toCheck = mutableMapOf()
        lastClicked = mutableMapOf()
        display = emptyList()
    }

    @SubscribeEvent
    fun onSlotClick(event: SlotClickEvent) {
        if (!isEnabled()) return
        superpairsPattern.matchMatcher(InventoryUtils.openInventoryName()) {
            currentExperiment = Experiments.entries.find { it.name == group("experiment") } ?: return

            val item = event.item ?: return
            if (isOutOfBounds(event.slotId, currentExperiment)) return

            toCheck.put(event.slotId, item)
            lastClicked.put(event.slotId, item.displayName)
        }
    }

    private fun drawDisplay() = buildList {
        val openInventory = InventoryUtils.getItemsInOpenChest()
        add("§6Experimentation Data")
        add("")
        for (pair in foundPairs) {
            add("§a${openInventory.get(pair.first.first).stack.getLore()[2]} - Found")
        }
        for (pair in foundMatches) {
            add("§e${openInventory.get(pair.first.first).stack.getLore()[2]} - Matched")
        }
        for (power in foundPowerUps) {
            add("§b${power.value} - Found")
        }
        add("")
        add("§l§cNot found")
        add("§ePairs - $possiblePairs")
        add("§7Normals - ${foundNormals.size}")
    }

    private fun isPowerUp(reward: String) =
        reward in listOf("Clicks", "Instant Find", "Click")

    private fun hasFoundPair(firstSlot: Int, secondSlot: Int, firstName: String, secondName: String) =
        firstSlot != secondSlot && firstName == secondName

    private fun hasFoundMatch(itemName: String, itemSlot: Int) =
        uncoveredItems.any { (slot, name) -> slot != itemSlot && name == itemName }

    private fun isOutOfBounds(slot: Int, experiment: Experiments) =
        slot <= startSlot || slot >= startSlot + experiment.gridSize + 6

    private fun isEnabled() =
        config.enabled && InventoryUtils.openInventoryName().startsWith("Superpairs (")

}
