package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.TimeUtils

object RemindCommand {

    fun command(args: Array<String>) {
        if (args.size < 2) {
            ChatUtils.userError("/shremind [time] [reason]")
            return
        }

        val time = TimeUtils.getDuration(args.first())
        val reason = args.drop(1).joinToString(" ")
        val remindAt = time.inWholeMilliseconds + System.currentTimeMillis()
    }
}
