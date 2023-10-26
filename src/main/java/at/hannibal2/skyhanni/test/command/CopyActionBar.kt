package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.data.ActionBarStatsData
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.transformIf
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor

object CopyActionBar {
    fun command(args: Array<String>) {
        val noColor = args.size == 1 && args[0] == "true"
        var string = ""
        string = ActionBarStatsData.actionBar.transformIf({noColor}) { removeColor() }

        OSUtils.copyToClipboard(string)
        LorenzUtils.chat("§e[SkyHanni] actionbar copied into your clipboard!")
    }
}
