package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.mixins.transformers.AccessorGuiPlayerTabOverlay
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.conditionalTransform
import at.hannibal2.skyhanni.utils.LorenzUtils.transformIf
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraft.client.Minecraft

object CopyTabListCommand {
    fun command(args: Array<String>) {
        val resultList = mutableListOf<String>()
        val noColor = args.size == 1 && args[0] == "true"
        for (line in TabListData.getTabList()) {
            val tabListLine = line.transformIf(noColor) { removeColor() }
            if (tabListLine != "") resultList.add("'$tabListLine'")
        }
        val tabList = Minecraft.getMinecraft().ingameGUI.tabList as AccessorGuiPlayerTabOverlay
        val tabHeader = tabList.header_skyhanni.conditionalTransform(noColor, { unformattedText }, { formattedText })
        val tabFooter = tabList.footer_skyhanni.conditionalTransform(noColor, { unformattedText }, { formattedText })
        val string = "Header:\n\n$tabHeader\n\nBody:\n\n${resultList.joinToString("\n")}\n\nFooter:\n\n$tabFooter"
        OSUtils.copyToClipboard(string)
        LorenzUtils.chat("§e[SkyHanni] Tab list copied into the clipboard!")
    }
}