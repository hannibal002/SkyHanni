package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PlayerSendChatEvent
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PlayerChatFilter {

    private val loggerPlayerChat = LorenzLogger("chat/player")

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.isOnHypixel) return
        if (!SkyHanniMod.feature.chat.playerMessages) return

        if (shouldBlock(event.message)) {
            event.blockedReason = "player_chat"
        }
    }

    private fun shouldBlock(originalMessage: String): Boolean {
        val split = if (originalMessage.contains("§7§r§7: ")) {
            originalMessage.split("§7§r§7: ")
        } else if (originalMessage.contains("§f: ")) {
            originalMessage.split("§f: ")
        } else {
            return false
        }

        var rawName = split[0]
        val channel = grabChannel(rawName)

        rawName = rawName.substring(channel.originalPrefix.length)
        val name = grabName(rawName) ?: return false

        val message = split[1]
        send(channel, name.removeColor(), message.removeColor())
        return true
    }

    private fun grabChannel(name: String): PlayerMessageChannel {
        return PlayerMessageChannel.values()
            .find { it != PlayerMessageChannel.ALL && name.startsWith(it.originalPrefix) } ?: PlayerMessageChannel.ALL
    }

    private fun grabName(rawName: String): String? {
        val nameSplit = rawName.split(" ")
        val last = nameSplit.last()
        val name = if (last.endsWith("]")) {
            nameSplit[nameSplit.size - 2]
        } else {
            last
        }

        val first = nameSplit[0]
        if (first != name) {
            if (!first.contains("VIP") && !first.contains("MVP")) {
                //TODO support yt + admin
                return null
            }
        }

        return name
    }

    private fun send(channel: PlayerMessageChannel, name: String, message: String) {
        loggerPlayerChat.log("[$channel] $name: $message")
        val event = PlayerSendChatEvent(channel, name, message)
        event.postAndCatch()

        if (event.cancelledReason != "") {
            loggerPlayerChat.log("cancelled: " + event.cancelledReason)
        } else {
            val finalMessage = event.message
            if (finalMessage != message) {
                loggerPlayerChat.log("message changed: $finalMessage")
            }

            val prefix = channel.prefix
            LorenzUtils.chat("$prefix §b$name §f$finalMessage")
        }
    }

}