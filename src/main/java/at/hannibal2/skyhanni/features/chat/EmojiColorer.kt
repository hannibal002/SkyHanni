package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class EmojiColorer {
    private val config get() = SkyHanniMod.feature.chat.EmojiConfig

    //maybe move this to repo
    private val emojis = arrayOf(
        "❤" to "§r§c❤",
        "✮" to "§r§6✮",
        "✔" to "§r§a✔",
        "✖" to "§r§c✖",
        "☕" to "§r§b☕",
        "➜" to "§r§e➜",
        "¯\\_(ツ)_/¯" to "§r§e¯\\_(ツ)_/¯",
        "(╯°□°）╯︵┻━┻" to "§r§c(╯°□°）╯§f︵§7 ┻━┻",
        "( ﾟ◡ﾟ)/" to "§r§d( ﾟ◡ﾟ)/",
        "123" to "§r§a1§e2§c3",
        "☉_☉" to "§r§b☉§e_§b☉",
        "✎..." to "§r§e✎§6...",
        "√(π+x)=L" to "§r§a√§e§l(§aπ§a§l+x§e§l)§a§l=§c§lL",
        "@\'-\'" to "§r§e@§a\'§e-§a\'",
        "(0.o?)" to "§r§6(§a0§6.§ao§c?§6)",
        "༼つ◕_◕༽つ" to "§r§b༼つ◕_◕༽つ",
        "('-')⊃━☆ﾟ.*･｡ﾟ" to "§r§e(§b'§e-§b'§e)⊃§c━§d☆ﾟ.*･｡ﾟ",
        "⚔" to "§r§e⚔",
        "✌" to "§r§a✌",
        "OOF" to "§r§c§lOOF",
        "<('O')>" to "§r§e§l<('O')>",
        "(ᵔᴥᵔ)" to "§r§6(ᵔᴥᵔ)",
        "ヽ (◕◡◕) ﾉ" to "§r§aヽ (◕◡◕) ﾉ",
        "= ＾● ⋏ ●＾ =" to "§r§e= §b＾● ⋏ ●＾§e =",
        "☃" to "§r§b☃",
        "(✿◠‿◠)" to "§r§e(§a✿§e◠‿◠)§7",
        "ヽ(^◇^*)/" to "§r§eヽ(^◇^*)/",
        "ヽ(⌐■_■)ノ♬" to "§r§9ヽ§5(§d⌐§c■§6_§e■§b)§3ノ§9♬",
        "(・⊝・)" to "§r§6(§8・§6⊝§8・§6)",
        "^-^" to "§r§a^-^",
        "<o/" to "§r§d<§eo§d/",
        "^_^" to "§r§a^_^"
    )

    private val resettingCodes = arrayOf(
        "§0",
        "§1",
        "§2",
        "§3",
        "§4",
        "§5",
        "§6",
        "§7",
        "§8",
        "§9",
        "§a",
        "§b",
        "§c",
        "§d",
        "§e",
        "§f"
    )

    private val formattingCodes = arrayOf(
        "§k",
        "§l",
        "§m",
        "§n",
        "§o",
        "§r"
    )

    @SubscribeEvent
    fun onChatReceive(event: LorenzChatEvent) {
        if (config.colorEmoji && EmojiReplacer.inArray(event.message.removeColor(), emojis)) {
            val newMessage = replace(event, emojis)
            event.blockedReason = "Emoji"
            LorenzUtils.chat(newMessage, false)
        }
    }

    private fun replace (event: LorenzChatEvent, array: Array<Pair<String, String>>): String {
        if (EmojiReplacer.inArray(event.message.removeColor(), array)) {
            var oldMessage = event.message
            array.forEach { (search, replace) ->
                if (oldMessage.contains(search)) {
                    val oldColor = findColor(oldMessage, search)
                    oldMessage = oldMessage.replace(search, "$replace$oldColor")
                }
            }
            return oldMessage
        }
        return event.message
    }

    private fun findColor(message: String, search: String): String {
        val lastIndex = message.indexOf(search)
        val firstIndex = findFirstIndex(message, lastIndex)
        val subString = message.substring(firstIndex,lastIndex)
        return handleColor(subString)
    }

    private fun handleColor(subString: String): String {
        var finalColors = "§${subString[1]}"
        for (code in formattingCodes) {
            if (subString.contains(code)) {
                finalColors += code
            }
        }
        return finalColors
    }

    private fun findFirstIndex(message: String, lastIndex: Int): Int {
        val subString = message.substring(0, lastIndex)
        var maxIndex = 0

        resettingCodes.forEach { code ->
            val index = subString.lastIndexOf(code)
            if (index > maxIndex) maxIndex = index
        }
        return maxIndex
    }
}
