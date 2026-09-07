package at.hannibal2.skyhanni.features.event.timing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.TimeUtils.formatTimeUntilWithDate
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object SeasonLockedChat {

    private val config get() = SkyHanniMod.feature.event.timing

    /**
     * REGEX-TEST: Jerry's Workshop is only available during the season of Late Winter!
     */
    private val seasonLockedPattern by RepoPattern.pattern(
        "event.timing.season.locked",
        "^(?<name>.+) is only available during the season of (?<season>.+)!$",
    )

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.seasonLockedMessages) return
        seasonLockedPattern.matchMatcher(event.cleanMessage) {
            val name = group("name")
            val month = SkyBlockTime.getSBMonthByName(group("season"))
            if (month == 0) return
            val start = nextStartOfMonth(month)
            // delayed so the line ends up below the message that triggered it
            DelayedRun.runNextTickEnd {
                ChatUtils.chat("§e$name §7opens in §b${start.formatTimeUntilWithDate()}")
            }
        }
    }

    private fun nextStartOfMonth(month: Int): SimpleTimeMark {
        val now = SkyBlockTime.now()
        // the start of the current month has already passed, so that month is only due again next year
        val year = if (month > now.month) now.year else now.year + 1
        return SkyBlockTime(year = year, month = month).toTimeMark()
    }
}
