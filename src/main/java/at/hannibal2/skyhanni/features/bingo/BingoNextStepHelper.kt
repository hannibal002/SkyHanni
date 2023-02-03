package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.data.HyPixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.SkillExperience
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileApiDataLoadedEvent
import at.hannibal2.skyhanni.features.bingo.nextstep.*
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.regex.Pattern

class BingoNextStepHelper {
    private var tick = 0
    private var dirty = true

    private val itemIslandRequired = mutableMapOf<String, IslandVisitStep>()
    private val itemRequired = mutableMapOf<String, NextStep>()
    private val islands = mutableMapOf<IslandType, IslandVisitStep>()

    companion object {
        private val finalSteps = mutableListOf<NextStep>()
        private val currentSteps = mutableListOf<NextStep>()
        val currentHelp = mutableListOf<String>()

        fun command() {
            updateResult(true)
        }

        private fun updateResult(print: Boolean = false) {
            if (print) println()
            currentSteps.clear()
            for (step in finalSteps) {
                printRequirements(step, print)
                if (print) println()
            }

            currentHelp.clear()
            currentHelp.add("Bingo Helper:")
            for (currentStep in currentSteps) {
                val text = getName(currentStep)
                currentHelp.add("  §7$text")
                if (print) println(text)
            }
            if (print) println()
        }

        private fun printRequirements(step: NextStep, print: Boolean, parentDone: Boolean = false, depth: Int = 0) {
            if (print) println(getName(step, parentDone, depth))
            var requirementsToDo = 0
            for (requirement in step.requirements) {
                printRequirements(requirement, print, step.done || parentDone, depth + 1)
                if (!requirement.done) {
                    requirementsToDo++
                }
            }

            if (!step.done && !parentDone) {
                if (requirementsToDo == 0) {
                    if (!currentSteps.contains(step)) {
                        currentSteps.add(step)
                    }
                }
            }
        }

        private fun getName(step: NextStep, parentDone: Boolean = false, depth: Int = 0): String {
            val prefix = "  ".repeat(depth) + if (step.done) "[DONE] " else if (parentDone) "[done] " else ""
            val suffix = if (step is ProgressionStep) progressDisplay(step) else ""
            return prefix + step.displayName + suffix
        }

        private fun progressDisplay(step: ProgressionStep): String {
            val having = step.amountHaving
            return if (having > 0) {
                val needed = step.amountNeeded
                val percentage = LorenzUtils.formatPercentage(having.toDouble() / needed)
                val havingFormat = LorenzUtils.formatInteger(having)
                val neededFormat = LorenzUtils.formatInteger(needed)
                " $percentage ($havingFormat/$neededFormat)"
            } else ""
        }
    }

    init {
        reset()
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!LorenzUtils.isBingoProfile) return
        if (event.phase != TickEvent.Phase.START) return

        tick++
        if (tick % 20 == 0) {
            update()
            updateIslandsVisited()
        }
        if (tick % 5 == 0) {
            updateCurrentSteps()
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!HyPixelData.skyBlock) return

        //TODO add thys message
//        if (event.message == "thys message") {
//            thys.done()
//        }
    }

    private fun updateCurrentSteps() {
        for (step in currentSteps.toMutableList()) {
            if (step is ItemsStep) {
                var totalCount = 0L
                for ((itemName, multiplier) in step.variants) {
                    val count = InventoryUtils.countItemsInLowerInventory { it.name?.removeColor() == itemName }
                    totalCount += count * multiplier
                }
                if (step.amountHaving != totalCount) {
                    step.amountHaving = totalCount
                    if (totalCount >= step.amountNeeded) {
                        step.done()
                    }
                    updateResult()
                }
            }
            if (step is SkillLevelStep) {
                val expForSkill = SkillExperience.getExpForSkill(step.skillName.lowercase())
                if (step.amountHaving != expForSkill) {
                    step.amountHaving = expForSkill
                    if (expForSkill >= step.amountNeeded) {
                        step.done()
                    }
                    updateResult()
                }
            }
        }
    }

    private fun NextStep.done(silent: Boolean = false) {
        if (done) return
        done = true
        updateResult()
        if (!silent) {
            LorenzUtils.chat("§e[SkyHanni] A bingo goal step is done! ($displayName)")
        }
    }

    private fun updateIslandsVisited() {
        for (step in islands.values) {
            val island = step.island
            if (island == LorenzUtils.skyBlockIsland) {
                step.done()
            }
        }
    }

    private fun update() {
        val personalGoals = BingoCardDisplay.personalGoals
        if (personalGoals.isEmpty()) {
            if (!dirty) {
                reset()
                dirty = true
            }
            return
        }

        if (!dirty) return
        dirty = false

        for (goal in personalGoals) {
            val description = goal.description.removeColor()

            val pattern = Pattern.compile("Reach ([0-9]+(?:,\\d+)*) (.*) Collection\\.")
            val matcher = pattern.matcher(description)
            if (matcher.matches()) {
                val amount = matcher.group(1).replace(",", "").toInt()
                val name = matcher.group(2)

                val collectionStep = CollectionStep(name, amount).apply { finalSteps.add(this) }
                createItemIslandRequirement(name, collectionStep)
                continue
            }
            if (description == "Craft an Emerald Ring.") {
                CraftStep("Emerald Ring").apply { finalSteps.add(this) } requires ItemsStep(
                    "32x Enchanted Emerald",
                    "Emerald",
                    160 * 32,
                    mapOf("Emerald" to 1, "Enchanted Emerald" to 160)
                ).apply { this requires IslandType.DWARVEN_MINES.getStep() }
            }
            if (description == "Obtain a Mathematical Hoe Blueprint.") {
                CraftStep("Mathematical Hoe Blueprint").apply { finalSteps.add(this) } requires ItemsStep(
                    "32x Jacob's Ticket",
                    "Jacob's Ticket",
                    32,
                    mapOf("Jacob's Ticket" to 1)
                ).apply { this requires IslandType.HUB.getStep() }.addItemRequirements()
            }

            println("No help for goal: '$description'")
        }

        updateResult()
    }

    @SubscribeEvent
    fun onProfileDataLoad(event: ProfileApiDataLoadedEvent) {
        val profileData = event.profileData

        val visitedZones = profileData["visited_zones"]?.asJsonArray ?: return
        for (element in visitedZones) {
            val zoneName = element.asString
            for (step in islands.values) {
                val island = step.island
                if (island.apiName == zoneName) {
                    step.done(true)
                }
            }
        }
    }

    private fun createItemIslandRequirement(itemName: String, step: NextStep): IslandVisitStep? {
        val islandReachStep = itemIslandRequired.getOrDefault(itemName, null)
        if (islandReachStep == null) {
            println("no island required for item: '$itemName'")
            return null
        }
        step requires islandReachStep
        return islandReachStep
    }

    private infix fun NextStep.requires(other: NextStep) {
        requirements.add(other)
    }

    private fun IslandType.getStep() = islands.getOrPut(this) { IslandVisitStep(this) }

    private fun reset() {
        islands.clear()
        finalSteps.clear()

        itemIslandRequired["Acacia Wood"] = IslandType.THE_PARK.getStep()
        itemIslandRequired["Redstone"] = IslandType.DEEP_CAVERNS.getStep()
        itemIslandRequired["Slimeball"] = IslandType.DEEP_CAVERNS.getStep()
        itemIslandRequired["Emerald"] = IslandType.DEEP_CAVERNS.getStep()
        itemIslandRequired["Mithril"] = IslandType.DEEP_CAVERNS.getStep()

        IslandType.GOLD_MINES.getStep() requires IslandType.HUB.getStep()
        IslandType.GOLD_MINES.getStep() requires SkillLevelStep("Mining", 1)
        IslandType.DEEP_CAVERNS.getStep() requires IslandType.GOLD_MINES.getStep()

        IslandType.DEEP_CAVERNS.getStep() requires SkillLevelStep("Mining", 5)

        val redstoneForThys = ItemsStep(
            "30x Enchanted Redstone (for Thys)",
            "Redstone",
            160 * 10 * 3,
            mapOf("Redstone" to 1, "Enchanted Redstone" to 160)
        ).apply { createItemIslandRequirement(itemName, this) }
        redstoneForThys requires IslandType.DEEP_CAVERNS.getStep()
        IslandType.DWARVEN_MINES.getStep() requires redstoneForThys
        IslandType.DWARVEN_MINES.getStep() requires SkillLevelStep("Mining", 12)
        IslandType.CRYSTAL_HOLLOWS.getStep() requires IslandType.DWARVEN_MINES.getStep()

        val farmingContest = ChatMessageStep("Farming Contest")
        farmingContest requires SkillLevelStep("Farming", 10)
        itemRequired["Jacob's Ticket"] = farmingContest


        enchantedCharcoal(7)
        compactor(7)
    }

    private fun compactor(amount: Long) {
        val compactorForMinions = ItemsStep(
            "Compactor (for Minions)",
            "Compactor",
            amount,
            mapOf("Compactor" to 1)
        ).apply { finalSteps.add(this) }

        compactorForMinions requires CollectionStep(
            "Cobblestone",
            2_500
        ).apply { this requires IslandType.HUB.getStep() }

        compactorForMinions requires ItemsStep(
            "" + (7 * amount) + " Enchanted Cobblestone (For Minions)",
            "Enchanted Cobblestone",
            amount * 7 * 160,
            mapOf("Cobblestone" to 1, "Enchanted Cobblestone" to 160)
        )
        compactorForMinions requires ItemsStep(
            "$amount Enchanted Redstone (For Minions)",
            "Enchanted Redstone",
            amount * 160,
            mapOf("Redstone" to 1, "Enchanted Redstone" to 160)
        )
    }

    private fun enchantedCharcoal(amount: Long) {
        val enchantedCharcoalForMinions = ItemsStep(
            "Enchanted Charcoal (for Minions)",
            "Enchanted Charcoal",
            amount,
            mapOf("Enchanted Charcoal" to 1)
        ).apply { finalSteps.add(this) }

        enchantedCharcoalForMinions requires CollectionStep(
            "Coal",
            2_500
        ).apply { this requires IslandType.GOLD_MINES.getStep() }

        enchantedCharcoalForMinions requires ItemsStep(
            "Oak Wood (For Minions)",
            "Oak Wood",
            amount * 32,
            mapOf("Oak Wood" to 1)
        )
        enchantedCharcoalForMinions requires ItemsStep(
            "Coal (For Minions)",
            "Coal",
            amount * 64 * 2,
            mapOf("Coal" to 1)
        )
    }

    private fun ItemsStep.addItemRequirements(): ItemsStep {
        val step = itemRequired[itemName]
        if (step != null) {
            requirements.add(step)
        }
        return this
    }
}
