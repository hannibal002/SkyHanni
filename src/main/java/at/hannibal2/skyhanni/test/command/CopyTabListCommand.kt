package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData

object CopyTabListCommand {
    fun command(args: Array<String>) {
        try {
            val resultList = mutableListOf<String>()
            var noColor = false
            if (args.size == 1) {
                if (args[0] == "true")  noColor = true
            }
            for (line in TabListData.getTabList()) {
                val tablistLine = if (noColor) line.removeColor() else line
                resultList.add("'$tablistLine'")
            }
            val string = resultList.joinToString("\n")
            OSUtils.copyToClipboard(string)
            LorenzUtils.chat("§e[SkyHanni] tablist copied into the clipboard!")
        }
        catch (_: Throwable) {
            LorenzUtils.chat("§c[SkyHanni] Nothing in tablist")
        }
    }
}