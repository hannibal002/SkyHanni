package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.SkillExperience
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileApiDataLoadedEvent
import at.hannibal2.skyhanni.features.bingo.nextstep.*
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class BingoNextStepHelper {
    private var tick = 0
    private var dirty = true

    private val itemIslandRequired = mutableMapOf<String, IslandVisitStep>()
    private val itemRequired = mutableMapOf<String, NextStep>()
    private val islands = mutableMapOf<IslandType, IslandVisitStep>()
    private val collectionPattern = "Reach (?<amount>[0-9]+(?:,\\d+)*) (?<name>.*) Collection\\.".toPattern()
    private val crystalPattern = "Obtain a (?<name>\\w+) Crystal in the Crystal Hollows\\.".toPattern()
    private val skillPattern = "Obtain level (?<level>.*) in the (?<skill>.*) Skill.".toPattern()

    companion object {
        private val finalSteps = mutableListOf<NextStep>()
        private val currentSteps = mutableListOf<NextStep>()
        var currentHelp = listOf<String>()

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

            currentHelp = drawDisplay(print)
        }

        private fun drawDisplay(print: Boolean): MutableList<String> {
            val newCurrentHelp = mutableListOf<String>()
            newCurrentHelp.add("§6Bingo Step Helper:")

            if (currentSteps.isEmpty()) {
                newCurrentHelp.add("§cOpen the §e/bingo §ccard.")
            }
            for (currentStep in currentSteps) {
                val text = getName(currentStep)
                newCurrentHelp.add("  §7$text")
                if (print) println(text)
            }
            if (print) println()
            return newCurrentHelp
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
        if (!SkyHanniMod.feature.bingo.cardDisplay) return
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

    var nextMessageIsCrystal = false

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.isBingoProfile) return
        if (!SkyHanniMod.feature.bingo.cardDisplay) return

        for (currentStep in currentSteps) {
            if (currentStep.displayName == "Obtain a Topaz Crystal") {
                if (event.message.matchRegex(" *§r§5§l✦ CRYSTAL FOUND §r§7\\(.§r§7/5§r§7\\)")) {
                    nextMessageIsCrystal = true
                    return
                }
                if (nextMessageIsCrystal) {
                    nextMessageIsCrystal = false
                    if (event.message.matchRegex(" *§r§eTopaz Crystal")) {
                        currentStep.done()
                    }
                }
            }
        }
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
            if (step is CollectionStep) {
                val counter = CollectionAPI.getCollectionCounter(step.collectionName)?.second ?: 0
                if (step.amountHaving != counter) {
                    step.amountHaving = counter
                    if (counter >= step.amountNeeded) {
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
            if (SkyHanniMod.feature.bingo.stepHelper) {
                LorenzUtils.chat("§e[SkyHanni] A bingo goal step is done! ($displayName)")
            }
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
        val personalGoals = BingoCardDisplay.personalGoals.filter { !it.done }
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
            readDescription(goal.description.removeColor())
        }

        updateResult()
    }

    private fun readDescription(description: String) {
        collectionPattern.matchMatcher(description) {
            val amount = group("amount").replace(",", "").toInt()
            val name = group("name")

            val collectionStep = CollectionStep(name, amount).apply { finalSteps.add(this) }
            createItemIslandRequirement(name, collectionStep)
            return
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
            ).apply { this requires IslandType.GARDEN.getStep() }.addItemRequirements()
        }

        crystalPattern.matchMatcher(description) {
            val crystal = group("name")
            ChatMessageStep("Obtain a $crystal Crystal").apply { finalSteps.add(this) } requires IslandType.CRYSTAL_HOLLOWS.getStep()
        }

        skillPattern.matchMatcher(description) {
            val level = group("level").toInt()
            val skill = group("skill")
            SkillLevelStep(skill, level).apply { finalSteps.add(this) }
        }

        println("No help for goal: '$description'")
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
        IslandType.DWARVEN_MINES.getStep() requires SkillLevelStep(
            "Mining",
            12
        ).also { it requires IslandType.THE_FARMING_ISLANDS.getStep() }
        IslandType.CRYSTAL_HOLLOWS.getStep() requires IslandType.DWARVEN_MINES.getStep()

        // TODO add skyblock level requirement
//        IslandType.GARDEN.getStep() requires SkyBlockLevelStep(6)
        IslandType.GARDEN.getStep() requires IslandType.HUB.getStep()

        val farmingContest = ChatMessageStep("Farming Contest")
        farmingContest requires SkillLevelStep("Farming", 10)
        itemRequired["Jacob's Ticket"] = farmingContest

        IslandType.DWARVEN_MINES.getStep().also { finalSteps.add(it) }
        ChatMessageStep("Get Ender Armor").also { finalSteps.add(it) } requires IslandType.THE_END.getStep()
        IslandType.THE_END.getStep() requires SkillLevelStep(
            "Combat",
            12
        ).also { it requires IslandType.DEEP_CAVERNS.getStep() }

//        enchantedCharcoal(7)
//        compactor(7)
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
