package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.onClick
import at.hannibal2.skyhanni.utils.chat.TextHelper.onHover
import at.hannibal2.skyhanni.utils.chat.TextHelper.send

object ColorFormattingHelper {
    fun printColorCodeList() {
        val text = mutableListOf<String>()
        text.add("§c=================== General Colors ===================")
        text.add("§f&0 = §0Black              §f&1 = §1Dark Blue")
        text.add("§f&2 = §2Dark Green      §f&3 = §3Dark Aqua")
        text.add("§f&4 = §4Dark Red         §f&5 = §5Dark Purple")
        text.add("§f&6 = §6Gold               §f&7 = §7Gray")
        text.add("§f&8 = §8Dark Gray       §f&9 = §9Blue")
        text.add("§f&a = §aGreen            §f&b = §bAqua")
        text.add("§f&c = §cRed               §f&d = §dLight Purple")
        text.add("§f&e = §eYellow            §f&f = §fWhite")
        text.add("§f&Z = §zChroma §r(needs to enable chroma setting)")
        text.add("§c================= Formatting Codes ==================")
        text.add("§f&k = Obfuscated (like this: §khellspawn§r)")
        text.add("§f&l = §lBold           §r&m = §mStrikethrough ")
        text.add("§f&o = §oItalic            §r&n = §nUnderline")
        text.add("§f&r = Reset")
        text.add("§c===================================================")
        text.add("§eClick to view extra info about colors and formatting.")

        val fullText = TextHelper.multiline(text)
        fullText.onHover("§eClick to see more!")
        fullText.onClick { printColorCodesExtra() }
        fullText.send()
    }

    private fun printColorCodesExtra() {
        ChatUtils.chat("§c================= Formatting Extra ==================", false)
        ChatUtils.clickableLinkChat(
            "§#§6§a§e§e§4§8§/[Click here to view codes on minecraft.wiki]",
            "https://minecraft.wiki/w/Formatting_codes#Color_codes",
            "§eOpen §cminecraft.wiki§e!",
            false,
            false,
        )
        ChatUtils.chat(
            "§eYou can also uses SkyHanni's system for any colors. " +
                "This is different from chroma. " +
                "Simply type §6&#&f&f&9&a&2&e&/ §efor color §#§f§f§9§a§2§e§/#ff9a2e§e " +
                "(adds §6& §ebefore every characters including §6#§e, ends with '§6&/§e').",
            false,
        )
        ChatUtils.clickableLinkChat(
            "§z[Click here to open color picker color-hex.com]",
            url = "https://www.color-hex.com",
            "§eOpen §ccolor-hex.com§e!",
            prefix = false,
        )
        ChatUtils.chat("§c===================================================", false)
    }
}
