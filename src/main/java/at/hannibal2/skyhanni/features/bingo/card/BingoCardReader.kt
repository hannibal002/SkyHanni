package at.hannibal2.skyhanni.features.bingo.card

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.jsonobjects.repo.BingoJson
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.bingo.BingoCardUpdateEvent
import at.hannibal2.skyhanni.events.bingo.BingoGoalReachedEvent
import at.hannibal2.skyhanni.features.bingo.BingoAPI
import at.hannibal2.skyhanni.features.bingo.card.goals.BingoGoal
import at.hannibal2.skyhanni.features.bingo.card.goals.GoalType
import at.hannibal2.skyhanni.features.bingo.card.goals.HiddenGoalData
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration

class BingoCardReader {
    private val config get() = SkyHanniMod.feature.event.bingo.bingoCard

    private val percentagePattern by RepoPattern.pattern("bingo.card.percentage", " {2}§8Top §.(?<percentage>.*)%")

    // TODO USE SH-REPO
    private val goalCompletePattern = "§6§lBINGO GOAL COMPLETE! §r§e(?<name>.*)".toPattern()
    private val personalHiddenGoalPattern = ".*§7§eThe next hint will unlock in (?<time>.*)".toPattern()

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryUpdatedEvent) {
        if (!LorenzUtils.isBingoProfile) return
        if (!config.enabled) return
        if (event.inventoryName != "Bingo Card") return

        for ((slot, stack) in event.inventoryItems) {
            val lore = stack.getLore()
            val goalType = when {
                lore.any { it.endsWith("Personal Goal") } -> GoalType.PERSONAL
                lore.any { it.endsWith("Community Goal") } -> GoalType.COMMUNITY
                else -> continue
            }
            val name = stack.name?.removeColor() ?: continue
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
            val communtyGoalPercentage = readCommuntyGoalPercentage(lore)

            val hiddenGoalData = getHiddenGoalData(name, description, goalType)
            val visualDescription = hiddenGoalData.tipNote

            val bingoGoal = BingoAPI.bingoGoals.getOrPut(slot) { BingoGoal() }
            with(bingoGoal) {
                this.type = goalType
                this.displayName = name
                this.description = visualDescription
                this.done = done
                this.hiddenGoalData = hiddenGoalData
                this.communtyGoalPercentage = communtyGoalPercentage
            }
        }
        BingoAPI.lastBingoCardOpenTime = SimpleTimeMark.now()

        BingoCardUpdateEvent().postAndCatch()
    }

    private fun readCommuntyGoalPercentage(lore: List<String>): Double? {
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

        val description = BingoAPI.getTip(name)?.getDescriptionLine()
        val tipNote = description?.let {
            unknownTip = false
            it
        } ?: originalDescription
        return HiddenGoalData(unknownTip, nextHintTime, tipNote)
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.isBingoProfile) return
        if (!config.enabled) return

        val name = goalCompletePattern.matchMatcher(event.message) {
            group("name")
        } ?: return

        val goal = BingoAPI.personalGoals.firstOrNull { it.displayName == name } ?: return
        goal.done = true
        BingoGoalReachedEvent(goal).postAndCatch()
        BingoCardUpdateEvent().postAndCatch()
    }

    private fun BingoJson.BingoTip.getDescriptionLine() = "§7" + note.joinToString(" ")
}
