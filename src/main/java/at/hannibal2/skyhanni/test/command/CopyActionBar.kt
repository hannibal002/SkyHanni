package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.data.ActionBarStatsData
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.transformIf
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor

object CopyActionBar {
    fun command(args: Array<String>) {
        val noColor = args.size == 1 && args[0] == "true"
        val string = ActionBarStatsData.actionBar.transformIf({ noColor }) { removeColor() }

        OSUtils.copyToClipboard(string)
        ChatUtils.chat("Actionbar copied into your clipboard!")
    }
}
