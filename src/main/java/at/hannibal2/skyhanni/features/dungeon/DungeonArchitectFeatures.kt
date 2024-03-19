package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.SackAPI
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class DungeonArchitectFeatures {

    private val config get() = SkyHanniMod.feature.dungeon
    private val patternGroup = RepoPattern.group("dungeon.copilot")

    private val puzzleFailPattern by patternGroup.pattern(
        "normal.puzzle.fail",
        "(?:§c§lPUZZLE FAIL!|§4) §.§.(?<name>\\S*) .*"
    )
    private val quizPuzzleFailPattern by patternGroup.pattern(
        "quiz.puzzle.fail",
        "§4\\[STATUE\\] Oruo the Omniscient§r§f: (?:§.)*(?<name>\\S*) (?:§.)*chose the wrong .*"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        if (!config.architectNotifier) return
        puzzleFailPattern.matchMatcher(event.message) {
            generateMessage(group("name"), event)
        }
        quizPuzzleFailPattern.matchMatcher(event.message) {
            generateMessage(group("name"), event)
        }
    }

    private fun generateMessage(key: String, event: LorenzChatEvent) {
        val architectItem = SackAPI.fetchSackItem("ARCHITECT_FIRST_DRAFT".asInternalName())
        if (architectItem.amount <= 0) return
        ChatUtils.clickableChat(
            "§c§lPUZZLE FAILED! §r§b$key §r§ehas failed a puzzle.\n§3§l[CLICK HERE TO GET ARCHITECT'S FIRST DRAFT] (${architectItem.amount}x left)§r§e",
            "/gfs ARCHITECT_FIRST_DRAFT 1",
            false
        )
        LorenzUtils.sendTitle("§c§lPUZZLE FAILED!", 3.seconds)
        event.blockedReason = "puzzle_fail"
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inDungeons && config.dungeonCopilot.enabled
    }

}
