package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.utils.ChatUtils
import net.minecraft.util.ChatComponentText

object ColorFormattingHelper {
    fun printColorCodeList() {
        ChatUtils.chat(
            ChatComponentText(
                "§c=================== General Colors ===================\n" +
                    "§f&0 = §0Black              §f&1 = §1Dark Blue\n" +
                    "§f&2 = §2Dark Green      §f&3 = §3Dark Aqua\n" +
                    "§f&4 = §4Dark Red         §f&5 = §5Dark Purple\n" +
                    "§f&6 = §6Gold               §f&7 = §7Gray\n" +
                    "§f&8 = §8Dark Gray       §f&9 = §9Blue\n" +
                    "§f&a = §aGreen            §f&b = §bAqua\n" +
                    "§f&c = §cRed               §f&d = §dLight Purple\n" +
                    "§f&e = §eYellow            §f&f = §fWhite\n" +
                    "§f&Z = §zChroma §r(needs to enable chroma setting)\n" +
                    "§c================= Formatting Codes ==================\n" +
                    "§f&k = Obfuscated (like this: §khellspawn§r)\n" +
                    "§f&l = §lBold           §r&m = §mStrikethrough \n" +
                    "§f&o = §oItalic            §r&n = §nUnderline\n" +
                    "§f&r = Reset\n"+
                    "§c==================================================="
            )
        )
        ChatUtils.clickableChat(
            "§eClick to view extra info about colors and formatting.",
            onClick = { printColorCodesExtra() },
            "§zClick to see more!",
            prefix = false
        )
    }

    private fun printColorCodesExtra() {
        ChatUtils.chat("§c================= Formatting Extra ==================", false)
        ChatUtils.clickableLinkChat(
            "§#§6§a§e§e§4§8§/[Click here to view codes on minecraft.wiki]",
            "https://minecraft.wiki/w/Formatting_codes#Color_codes",
            "§fOpen minecraft.wiki",
            false,
            false
        )
        ChatUtils.chat(
            "§eYou can also uses SkyHanni's system for any colors. " +
                "This is different from chroma. " +
                "Simply type &#&f&f&9&a&2&e&/ for color §#§f§f§9§a§2§e§/#ff9a2e§e " +
                "(adds & before every characters including #, ends with '&/').",
            false
        )
        ChatUtils.clickableLinkChat(
            "§z[Click here to open color picker color-hex.com]",
            url = "https://www.color-hex.com",
            "§fOpen color-hex.com",
            prefix = false
        )
        ChatUtils.chat("§c===================================================", false)
    }
}
