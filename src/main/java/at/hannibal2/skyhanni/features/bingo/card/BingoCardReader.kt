package at.hannibal2.skyhanni.features.bingo.card

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.jsonobjects.repo.BingoJson
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.bingo.BingoCardUpdateEvent
import at.hannibal2.skyhanni.events.bingo.BingoGoalReachedEvent
import at.hannibal2.skyhanni.features.bingo.BingoAPI
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration

class BingoCardReader {
    private val config get() = SkyHanniMod.feature.event.bingo.bingoCard

    // TODO USE SH-REPO
    private val goalCompletePattern = "§6§lBINGO GOAL COMPLETE! §r§e(?<name>.*)".toPattern()

    private val personalHiddenGoalPattern = ".*§7§eThe next hint will unlock in (?<time>.*)".toPattern()

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.isBingoProfile) return
        if (!config.enabled) return
        if (event.inventoryName != "Bingo Card") return

        BingoAPI.bingoGoals.clear()
        for ((slot, stack) in event.inventoryItems) {
            val isPersonalGoal = stack.getLore().any { it.endsWith("Personal Goal") }
            val isCommunityGoal = stack.getLore().any { it.endsWith("Community Goal") }
            if (!isPersonalGoal && !isCommunityGoal) continue
            val name = stack.name?.removeColor() ?: continue
            val lore = stack.getLore()
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

            val done = stack.getLore().any { it.contains("GOAL REACHED") }

            val goalType = if (isPersonalGoal) GoalType.PERSONAL else GoalType.COMMUNITY
            val hiddenGoalData = getHiddenGoalData(name, description, goalType)
            val visualDescription = hiddenGoalData.tipNote

            val bingoGoal = BingoGoal(name, visualDescription, goalType, slot, done, hiddenGoalData)
            BingoAPI.bingoGoals.add(bingoGoal)
        }
        BingoAPI.lastBingoCardOpenTime = SimpleTimeMark.now()

        BingoCardUpdateEvent().postAndCatch()
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
