package at.hannibal2.skyhanni.features.chat.playerchat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.MarkedPlayerManager
import net.minecraft.util.ChatComponentText
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PlayerChatModifier {

    private val patterns = mutableListOf<Regex>()

    init {
        patterns.add("§(?:a|b|6)\\[(?:VIP|MVP)(?:(?:§.|\\+)*)](?: {1,2})(?:§[7ab6])?(\\w{2,16})".toRegex()) // ranked player everywhere
        patterns.add("§(?:7|a|b|6)((?:\\w+){2,16})§r(?!§7x)".toRegex()) // nons in notification message
        patterns.add("(?:§7 )?§7((?:\\w+){2,16})§7§r".toRegex()) // nons user chat
    }

    @SubscribeEvent
    fun onChatReceive(event: LorenzChatEvent) {
        val foundCommands = mutableListOf<IChatComponent>()
        val message = event.chatComponent

        addComponent(foundCommands, event.chatComponent)
        for (sibling in message.siblings) {
            addComponent(foundCommands, sibling)
        }

        val size = foundCommands.size
        if (size > 1) {
            return
        }
        val original = event.chatComponent.formattedText
        val newText = cutMessage(original)
        if (original == newText) return


        val text = ChatComponentText(newText)
        if (size == 1) {
            val chatStyle = foundCommands[0].chatStyle
            text.chatStyle.chatClickEvent = chatStyle.chatClickEvent
            text.chatStyle.chatHoverEvent = chatStyle.chatHoverEvent
        }
        event.chatComponent = text
    }

    private fun addComponent(foundCommands: MutableList<IChatComponent>, message: IChatComponent) {
        val clickEvent = message.chatStyle.chatClickEvent
        if (clickEvent != null) {
            if (foundCommands.size == 1) {
                if (foundCommands[0].chatStyle.chatClickEvent.value == clickEvent.value) {
                    return
                }
            }
            foundCommands.add(message)
        }
    }

    private fun cutMessage(input: String): String {
        var string = input

        //all players same color in chat
        string = string.replace("§r§7: ", "§r§f: ")

        if (SkyHanniMod.feature.chat.chatFilter) {
            if (string.contains("§r§f: ")) {
                if (PlayerChatFilter.shouldChatFilter(string)) {
                    string = string.replace("§r§f: ", "§r§7: ")
                }
            }
        }

        if (SkyHanniMod.feature.chat.playerRankHider) {
            for (pattern in patterns) {
                string = string.replace(pattern, "§b$1")
            }
            string = string.replace("§(?:7|a|b|6)((?:\\w+){2,16})'s", "§b$1's")
            string = string.replace("§(?:7|a|b|6)((?:\\w+){2,16}) (§.)", "§b$1 $2")
        }

        if (SkyHanniMod.feature.markedPlayers.highlightInChat) {
            for (markedPlayer in MarkedPlayerManager.playerNamesToMark) {
                string = string.replace(markedPlayer, "§e$markedPlayer§r")
            }
        }

        return string
    }

//    private fun shouldChatFilter(input: String): Boolean {
//        val text = input.lowercase()
//
//        //Low baller
//        if (text.contains("lowballing")) return true
//        if (text.contains("lowballer")) return true
//
//        //Trade
//        if (text.contains("buy")) return true
//        if (text.contains("sell")) return true
//        if (text.contains("on my ah")) return true
//
//
//        return false
//    }
}
