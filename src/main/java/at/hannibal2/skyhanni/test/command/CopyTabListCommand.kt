package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraft.client.Minecraft

object CopyTabListCommand {
    fun command(args: Array<String>) {
        try {
            val resultList = mutableListOf<String>()
            val noColor = args.size == 1 && args[0] == "true"
            for (line in TabListData.getTabList()) {
                val tabListLine = if (noColor) line.removeColor() else line
                if (tabListLine != "") resultList.add("'$tabListLine'")
            }
            val tabHeader = Minecraft.getMinecraft().ingameGUI.tabList.header.formattedText
            val tabFooter = Minecraft.getMinecraft().ingameGUI.tabList.footer.formattedText
            val string = "Header:\n\n" + tabHeader.toString() + "\n\nBody:\n\n" + resultList.joinToString("\n") + "\nFooter:\n\n" + tabFooter.toString()
            OSUtils.copyToClipboard(string)
            LorenzUtils.chat("§e[SkyHanni] tablist copied into the clipboard!")
        }
        catch (_: Throwable) {
            LorenzUtils.chat("§c[SkyHanni] Nothing in tablist")
        }
    }
}