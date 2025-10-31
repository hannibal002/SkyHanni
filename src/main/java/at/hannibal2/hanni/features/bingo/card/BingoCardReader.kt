package at.hannibal2.hanni.features.bingo.card

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.jsonobjects.repo.BingoData
import at.hannibal2.hanni.events.InventoryUpdatedEvent
import at.hannibal2.hanni.events.bingo.BingoCardUpdateEvent
import at.hannibal2.hanni.events.bingo.BingoGoalReachedEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.features.bingo.BingoApi
import at.hannibal2.hanni.features.bingo.card.goals.BingoGoal
import at.hannibal2.hanni.features.bingo.card.goals.GoalType
import at.hannibal2.hanni.features.bingo.card.goals.HiddenGoalData
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils.removeColor
import at.hannibal2.hanni.utils.TimeUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration

@HanniModule
object BingoCardReader {

    private val config get() = HanniMod.feature.event.bingo.bingoCard
    private val patternGroup = RepoPattern.group("bingo.card")
    private val percentagePattern by patternGroup.pattern(
        "percentage",
        " {2}§8Top §.(?<percentage>.*)%"
    )
    private val goalCompletePattern by patternGroup.pattern(
        "goalcomplete",
        "§6§lBINGO GOAL COMPLETE! §r§e(?<name>.*)"
    )
    private val personalHiddenGoalPattern by patternGroup.pattern(
        "hiddengoal",
        ".*§7§eThe next hint will unlock in (?<time>.*)"
    )

    @HandleEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!config.enabled) return
        if (event.inventoryName != "Bingo Card") return

        for ((slot, stack) in event.inventoryItems) {
            val lore = stack.getLore()
            val goalType = when {
                lore.any { it.endsWith("Personal Goal") } -> GoalType.PERSONAL
                lore.any { it.endsWith("Community Goal") } -> GoalType.COMMUNITY
                else -> continue
            }
            val name = stack.displayName.removeColor()
            var index = 0
            val builder = StringBuilder()
            for (s in lore) {
                if (index > 1) {
                    if (s == "") break
                    builder.append(s)
                    builder.append(" ")
                }
                index++
            }
            var description = builder.toString()
            if (description.endsWith(" ")) {
                description = description.substring(0, description.length - 1)
            }
            if (description.startsWith("§7§7")) {
                description = description.substring(2)
            }

            val done = lore.any { it.contains("GOAL REACHED") }
            val communityGoalPercentage = readCommunityGoalPercentage(lore)
            val hiddenGoalData = getHiddenGoalData(name, description, goalType)
            val visualDescription = hiddenGoalData.tipNote

            val guide = BingoApi.getData(name)?.guide?.map { "§7$it" } ?: listOf("§cNo guide yet!")

            val bingoGoal = BingoApi.bingoGoals.getOrPut(slot) { BingoGoal() }

            with(bingoGoal) {
                this.type = goalType
                this.displayName = name
                this.description = visualDescription
                this.guide = guide
                this.done = done
                this.hiddenGoalData = hiddenGoalData
            }
            communityGoalPercentage?.let {
                bingoGoalDifference(bingoGoal, it)
                bingoGoal.communtyGoalPercentage = it
            }
        }
        BingoApi.lastBingoCardOpenTime = SimpleTimeMark.now()

        BingoCardUpdateEvent.post()
    }

    private fun bingoGoalDifference(bingoGoal: BingoGoal, new: Double) {
        val old = bingoGoal.communtyGoalPercentage ?: 1.0

        if (!config.communityGoalProgress) return
        if (new == old) return

        val oldFormat = BingoApi.getCommunityPercentageColor(old)
        val newFormat = BingoApi.getCommunityPercentageColor(new)
        val color = if (new > old) "§c" else "§a"
        ChatUtils.chat("$color${bingoGoal.displayName}: $oldFormat §b->" + " $newFormat")
    }

    private fun readCommunityGoalPercentage(lore: List<String>): Double? {
        for (line in lore) {
            percentagePattern.matchMatcher(line) {
                return group("percentage").toDouble() / 100
            }
        }

        return null
    }

    private fun getHiddenGoalData(
        name: String,
        originalDescription: String,
        goalType: GoalType,
    ): HiddenGoalData {
        var unknownTip = false
        val nextHintTime: Duration? = when (goalType) {
            GoalType.PERSONAL -> {
                personalHiddenGoalPattern.matchMatcher(originalDescription) {
                    unknownTip = true
                    TimeUtils.getDuration(group("time").removeColor())
                }
            }

            GoalType.COMMUNITY -> {
                if (originalDescription == "§7This goal will be revealed §7when it hits Tier IV.") {
                    unknownTip = true
                }
                null
            }
        }

        val description = BingoApi.getData(name)?.getDescriptionLine()
        val tipNote = description?.let {
            unknownTip = false
            it
        } ?: originalDescription
        return HiddenGoalData(unknownTip, nextHintTime, tipNote)
    }

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!SkyBlockUtils.isBingoProfile) return
        if (!config.enabled) return

        val name = goalCompletePattern.matchMatcher(event.message) {
            group("name")
        } ?: return

        val goal = BingoApi.personalGoals.firstOrNull { it.displayName == name } ?: return
        goal.done = true
        BingoGoalReachedEvent(goal).post()
        BingoCardUpdateEvent.post()
    }

    private fun BingoData.getDescriptionLine() = "§7" + note.joinToString(" ")
}
