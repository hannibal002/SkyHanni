package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.SackAPI.getAmountInSacks
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class DungeonArchitectFeatures {

    private val config get() = SkyHanniMod.feature.dungeon
    private val patternGroup = RepoPattern.group("dungeon.architectsdraft")

    private val puzzleFailPattern by patternGroup.pattern(
        "puzzle.fail.normal",
        "(?:§c§lPUZZLE FAIL!|§4) §.§.(?<name>\\S*) .*"
    )
    private val quizPuzzleFailPattern by patternGroup.pattern(
        "puzzle.fail.quiz",
        "§4\\[STATUE] Oruo the Omniscient§r§f: (?:§.)*(?<name>\\S*) (?:§.)*chose the wrong .*"
    )

    private val architectsFirstDraftItem = "ARCHITECT_FIRST_DRAFT".asInternalName()

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        puzzleFailPattern.matchMatcher(event.message) {
            generateMessage(group("name"), event)
        }
        quizPuzzleFailPattern.matchMatcher(event.message) {
            generateMessage(group("name"), event)
        }
    }

    private fun generateMessage(name: String, event: LorenzChatEvent) {
        val architectItemAmount = architectsFirstDraftItem.getAmountInSacks()
        if (architectItemAmount <= 0) return

        ChatUtils.clickableChat(
            "§c§lPUZZLE FAILED! §r§b$name §r§efailed a puzzle. \n" +
                "§eClick here to get §5Architect's First Draft §7(§e${architectItemAmount}x left§7)",
            { HypixelCommands.getFromSacks("ARCHITECT_FIRST_DRAFT", 1) },
            prefix = false
        )
        LorenzUtils.sendTitle("§c§lPUZZLE FAILED!", 3.seconds)
        event.blockedReason = "puzzle_fail"
    }

    private fun isEnabled(): Boolean = DungeonAPI.inDungeon() && config.architectNotifier
}
