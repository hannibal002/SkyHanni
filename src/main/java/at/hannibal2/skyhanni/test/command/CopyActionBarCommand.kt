package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.data.ActionBarData
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor

object CopyActionBarCommand {
    fun command(args: Array<String>) {
        val noFormattingCodes = args.size == 1 && args[0] == "true"
        val (actionBar, status) = if (noFormattingCodes) Pair(ActionBarData.getActionBar().removeColor(), "without") else Pair(ActionBarData.getActionBar(), "with")

        OSUtils.copyToClipboard(actionBar)
        LorenzUtils.chat("Action bar name copied to clipboard $status formatting codes!")
    }
}
