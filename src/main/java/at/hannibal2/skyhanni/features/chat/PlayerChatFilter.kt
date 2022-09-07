package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PlayerSendChatEvent
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class PlayerChatFilter {

    private val loggerPlayerChat = LorenzLogger("chat/player")

    //§8[§9109§8] §b[MVP§c+§b] 4Apex§f§r§f: omg selling
    private val patternSkyBlockLevel = Pattern.compile("§8\\[§(.)(\\d+)§8] (.+)")

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.isOnHypixel) return

        if (shouldBlock(event.message)) {
            event.blockedReason = "player_chat"
        }
    }

    private fun shouldBlock(originalMessage: String): Boolean {
        //since hypixel sends own chat messages really weird " §r§8[§r§d205§r§8] §r§6[MVP§r§c++§r§6] hannibal2"
        var rawMessage = originalMessage.replace("§r", "").trim()

        val matcher = patternSkyBlockLevel.matcher(rawMessage)
        if (matcher.matches()) {
            SkyBlockLevelChatMessage.setData(matcher.group(2).toInt(), matcher.group(1))
            rawMessage = matcher.group(3)
        }

        val split = if (rawMessage.contains("§7§7: ")) {
            rawMessage.split("§7§7: ")
        } else if (rawMessage.contains("§f: ")) {
            rawMessage.split("§f: ")
        } else {
            return false
        }

        var rawName = split[0]
        val channel = grabChannel(rawName)

        rawName = rawName.substring(channel.originalPrefix.length)
        val name = grabName(rawName) ?: return false

        if (!SkyHanniMod.feature.chat.playerMessagesFormat) {
            if (SkyHanniMod.feature.chat.hideSkyblockLevel) {
                LorenzUtils.chat(rawMessage)
                return true
            }
            return false
        }

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