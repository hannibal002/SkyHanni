package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class EmojiReplacer {
    //maybe move this to repo?
    private val mvpPlusPlus = arrayOf(
        "<3" to "❤",
        ":star" to "✮",
        ":yes:" to "✔",
        ":no:" to "✖",
        ":java:" to "☕",
        ":arrow:" to "➜",
        ":shrug:" to "¯\\_(ツ)_/¯",
        ":tableflip:" to "(╯°□°）╯︵┻━┻",
        "o/" to "( ﾟ◡ﾟ)/",
//        ":123:" to "123",
        ":totem:" to "☉_☉",
        ":typing:" to "✎...",
        ":maths:" to "√(π+x)=L",
        ":snail:" to "@\'-\'",
        ":thinking:" to "(0.o?)",
        ":gimme:" to "༼つ◕_◕༽つ",
        ":wizard:" to "('-')⊃━☆ﾟ.*･｡ﾟ",
        ":pvp:" to "⚔",
        ":peace:" to "✌",
        ":oof:" to "OOF",
        ":puffer:" to "<('O')>"
    )

    private val gifted5 = arrayOf(
//        "^-^" to "^-^",
        ":cute:" to "(✿◠‿◠)"
    )

    private val gifted20 = arrayOf(
        ":dab:" to "<o/",
        ":yey:" to "ヽ (◕◡◕) ﾉ"
    )

    private val gifted50 = arrayOf(
        ":dj:" to "ヽ(⌐■_■)ノ♬",
        ":dog:" to "(ᵔᴥᵔ)"
    )

    private val gifted100 = arrayOf(
        ":cat:" to "= ＾● ⋏ ●＾ =",
        "h/" to "ヽ(^◇^*)/"
    )
    private val gifted200 = arrayOf(
        ":sloth:" to "(・⊝・)",
        ":snow:" to "☃"
    )

    private val config get() = SkyHanniMod.feature.chat.EmojiConfig

    @SubscribeEvent
    fun onChatSend(event: MessageSendToServerEvent) {
        if (config.enabled) {
            var arrayFinal: Array<Pair<String, String>> = emptyArray()
            if (config.emojiReplace.mvp) arrayFinal += mvpPlusPlus
            if (config.emojiReplace.five) arrayFinal += gifted5
            if (config.emojiReplace.twenty) arrayFinal += gifted20
            if (config.emojiReplace.fifty) arrayFinal += gifted50
            if (config.emojiReplace.hundred) arrayFinal += gifted100
            if (config.emojiReplace.twoHundred) arrayFinal += gifted200
            chatEdit(event, arrayFinal)
        }
    }

    private fun chatEdit(event: MessageSendToServerEvent, array: Array<Pair<String, String>>) {
        if (inArray(event.message, array)) {
            event.isCanceled = true
            var newMessage = event.message
            array.forEach { (search, replace) ->
                newMessage = newMessage.replace(search, replace)
            }
            LorenzUtils.sendMessageToServer(newMessage)
        } else return
    }

    companion object {
        fun inArray(input: String, array: Array<Pair<String, String>>): Boolean {
            for ((leftHalf, _) in array) {
                if (input.contains(leftHalf)) return true
            }
            return false
        }

    }
}
