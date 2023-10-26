package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class ServerRestartTitle {
    private val config get() = SkyHanniMod.feature.misc

    // TODO USE SH-REPO
    private val pattern = "§cServer closing: (?<minutes>\\d+):(?<seconds>\\d+) §8.*".toPattern()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.serverRestartTitle) return

        if (!event.repeatSeconds(1)) return

        for (line in ScoreboardData.sidebarLinesFormatted) {
            pattern.matchMatcher(line) {
                val minutes = group("minutes").toInt()
                val seconds = group("seconds").toInt()
                val totalSeconds = minutes * 60 + seconds
                if (totalSeconds > 120 && totalSeconds % 30 != 0) return
                val time = TimeUtils.formatDuration(totalSeconds.toLong() * 1000)
                LorenzUtils.sendTitle("§cServer Restart in §b$time", 2.seconds)
            }
        }
    }
}
