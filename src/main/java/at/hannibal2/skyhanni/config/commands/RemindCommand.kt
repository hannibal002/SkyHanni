package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.util.Locale

object RemindCommand {
    private val commandPattern by RepoPattern.pattern(
        "remind.command",
        "(?<reason>\\w+)\\s+(?<time>\\d+[mhd])"
    )
    private val timePattern by RepoPattern.pattern(
        "remind.time",
        "(?<amount>\\d+)(?<type>[mhd])"
    )

    fun command(args: Array<String>) {
        if (args.size < 2) {
            ChatUtils.userError("/shremind [reason] [time]")
            return
        }
        val (reason, time) = commandPattern.matchMatcher(args.joinToString(" ").lowercase()) {
            this.group("reason") to this.group("time")
        }?: return
        val (amount, type) = timePattern.matchMatcher(time.lowercase()) {
            this.group("amount").toInt() to this.group("type")
        }?: return

        val mins = convertToMins(amount, type)
        if (mins > 10080) {
            ChatUtils.userError("You can not set a reminder that is longer than 7 days!")
            return
        }
        if (timePattern.matches(reason.lowercase())) {
            ChatUtils.userError("/shremind [reason] [time]")
            return
        }

    }

    private fun convertToMins(amount: Int, type: String): Int {
        if (type == "m") return amount
        if (type == "h") return amount * 60
        if (type == "d") return amount * 1440
        return 0
    }
}
