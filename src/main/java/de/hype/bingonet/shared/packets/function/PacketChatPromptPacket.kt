package de.hype.bingonet.shared.packets.function

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.objects.Message
import de.hype.bingonet.shared.objects.Message.Companion.tellraw
import org.apache.commons.text.StringEscapeUtils

class PacketChatPromptPacket(val packets: MutableList<AbstractPacket>, private val message: String) :
    AbstractPacket() {
    val printMessage: Message
        get() {
            val base =
                "[\"\",{\"text\":\"Bingo Net Server: \",\"color\":\"gold\"},\"\\\"${StringEscapeUtils.escapeJson(message)}\\\" \",\"press (\",{\"keybind\":\"Chat Prompt Yes / Open Menu\",\"color\":\"green\"},\") to perform.\"]"
            return tellraw(base)
        }
}
