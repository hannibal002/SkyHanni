package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ServerRestartTitle {

    private val config get() = SkyHanniMod.feature.misc

    companion object {
        private val restartingGroup = RepoPattern.group("features.misc.serverrestart")
        private val restartingPattern by restartingGroup.pattern(
            "time",
            "§cServer closing: (?<minutes>\\d+):(?<seconds>\\d+) ?§8.*"
        )
        val restartingGreedyPattern by restartingGroup.pattern(
            "greedy",
            "§cServer closing.*"
        )
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.serverRestartTitle) return

        if (!event.repeatSeconds(1)) return

        for (line in ScoreboardData.sidebarLinesFormatted) {
            restartingPattern.matchMatcher(line) {
                try {
                    val minutes = group("minutes").toInt().minutes
                    val seconds = group("seconds").toInt().seconds
                    val totalTime = minutes + seconds
                    if (totalTime > 2.minutes && totalTime.inWholeSeconds % 30 != 0L) return
                    val time = totalTime.format()
                    LorenzUtils.sendTitle("§cServer Restart in §b$time", 2.seconds)
                } catch (e: Throwable) {
                    ErrorManager.logErrorWithData(
                        e, "Error reading server restart time from scoreboard",
                        "line" to line,
                        "restartPattern" to restartingPattern.pattern(),
                    )
                }
                return
            }
        }
    }
}
