package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
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
            val key = group("name")
            ChatUtils.clickableChat(
                "§c§lPUZZLE FAILED! §r§b$key §r§ehas failed a puzzle.\n§3§l[CLICK HERE TO GET ARCHITECT]",
                "/gfs ARCHITECT_FIRST_DRAFT 1",
                false
            )
            LorenzUtils.sendTitle("§c§lPUZZLE FAILED!", 3.seconds)
            event.blockedReason = "puzzle_fail"
        }
        quizPuzzleFailPattern.matchMatcher(event.message) {
            val key = group("name")
            ChatUtils.clickableChat(
                "§c§lPUZZLE FAILED! §r§b$key §r§ehas failed a puzzle.\n§3§l[CLICK HERE TO GET ARCHITECT]",
                "/gfs ARCHITECT_FIRST_DRAFT 1",
                false
            )
            LorenzUtils.sendTitle("§c§lPUZZLE FAILED!", 3.seconds)
            event.blockedReason = "puzzle_fail"
        }
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inDungeons && config.dungeonCopilot.enabled
    }

}
