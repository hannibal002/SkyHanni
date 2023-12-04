package at.hannibal2.skyhanni.features.bingo.card

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.bingo.BingoCardUpdateEvent
import at.hannibal2.skyhanni.events.bingo.BingoGoalReachedEvent
import at.hannibal2.skyhanni.features.bingo.BingoAPI
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BingoCardReader {
    private val config get() = SkyHanniMod.feature.event.bingo.bingoCard

    // TODO USE SH-REPO
    private val goalCompletePattern = "§6§lBINGO GOAL COMPLETE! §r§e(?<name>.*)".toPattern()

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
}
