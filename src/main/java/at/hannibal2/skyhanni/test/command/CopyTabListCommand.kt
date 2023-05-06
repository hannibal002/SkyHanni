package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData

object CopyTabListCommand {
    fun command(args: Array<String>) {
        try {
            val resultList = mutableListOf<String>()
            val noColor = args.size == 1 && args[0] == "true"
            for (line in TabListData.getTabList()) {
                val tabListLine = if (noColor) line.removeColor() else line
                resultList.add("'$tabListLine'")
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