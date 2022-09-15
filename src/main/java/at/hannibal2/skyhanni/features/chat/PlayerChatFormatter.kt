package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PlayerSendChatEvent
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraft.event.ClickEvent
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class PlayerChatFormatter {

    private val loggerPlayerChat = LorenzLogger("chat/player")

    //§6[⌬57] §r§8[§r§b235§r§8] §r§6[MVP§r§c++§r§6] hannibal2§r§f: Hello World!
    private val patternElitePrefix = Pattern.compile("§6\\[⌬(\\d+)] (.+)")

    //§8[§9109§8] §b[MVP§c+§b] 4Apex§f§r§f: omg selling
    private val patternSkyBlockLevel = Pattern.compile("§8\\[§(.)(\\d+)§8] (.+)")

    //§dTo §r§b[MVP§r§3+§r§b] Skyfall55§r§7: §r§7hello :)
    private var patternPrivateMessage: Pattern = Pattern.compile("§d(To|From) §r(.+)§r§7: §r§7(.+)")

    private val patternUrl =
        Pattern.compile("^https?:\\/\\/(?:www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&\\/=]*)$")

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.isOnHypixel) return

        if (shouldBlock(event.message)) {
            event.blockedReason = "player_chat"
        }
    }

    private fun shouldBlock(originalMessage: String): Boolean {
        if (handlePrivateMessage(originalMessage)) return true

        //since hypixel sends own chat messages really weird " §r§8[§r§d205§r§8] §r§6[MVP§r§c++§r§6] hannibal2"
        var rawMessage = originalMessage.replace("§r", "").trim()

        val matcherElite = patternElitePrefix.matcher(rawMessage)
        val elitePrefix = if (matcherElite.matches()) {
            val elitePosition = matcherElite.group(1).toInt()
            rawMessage = matcherElite.group(2)
            when (SkyHanniMod.feature.chat.eliteFormat) {
                0 -> "§6[⌬$elitePosition] "
                1 -> "§6§l⌬$elitePosition "
                2 -> ""
                else -> ""
            }
        } else {
            ""
        }


        val level: Int
        val levelColor: String
        val matcher = patternSkyBlockLevel.matcher(rawMessage)
        if (matcher.matches()) {
            levelColor = matcher.group(1)
            level = matcher.group(2).toInt()
            rawMessage = matcher.group(3)
        } else {
            level = -1
            levelColor = ""
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

        val message = split[1]
        callEvent(channel, name, message.removeColor(), level, levelColor, elitePrefix)
        return true
    }

    private fun handlePrivateMessage(originalMessage: String): Boolean {
        val matcher = patternPrivateMessage.matcher(originalMessage)
        if (!matcher.matches()) return false
        val direction = matcher.group(1)
        val rawName = matcher.group(2)
        val name = grabName(rawName) ?: return false

        val message = matcher.group(3)
        val colon = if (SkyHanniMod.feature.chat.playerColonHider) "" else ":"
        LorenzUtils.chat("§d$direction $name§f$colon $message")
        loggerPlayerChat.log("[Msg_$direction] $name: $message")
        return true
    }

    private fun grabChannel(name: String): PlayerMessageChannel {
        return PlayerMessageChannel.values()
            .find { it != PlayerMessageChannel.ALL && name.startsWith(it.originalPrefix) }
            ?: PlayerMessageChannel.ALL
    }

    private fun grabName(rawName: String): String? {
        val nameSplit = rawName.removeColor().split(" ")
        val last = nameSplit.last()
        val cleanName = if (last.endsWith("]")) {
            nameSplit[nameSplit.size - 2]
        } else {
            last
        }

        val first = nameSplit[0]
        if (first != cleanName) {
            if (!first.contains("VIP") && !first.contains("MVP")) {
                //TODO support yt + admin
                return null
            }
        }
        return if (SkyHanniMod.feature.chat.playerRankHider) {
            "§b$cleanName"
        } else {
            rawName
        }
    }

    private fun callEvent(
        channel: PlayerMessageChannel,
        name: String,
        message: String,
        level: Int,
        levelColor: String,
        elitePrefix: String,
    ) {
        loggerPlayerChat.log("[$channel] ${name.removeColor()}: $message")
        val event = PlayerSendChatEvent(channel, name, message)
        event.postAndCatch()

        if (event.cancelledReason != "") {
            loggerPlayerChat.log("cancelled: " + event.cancelledReason)
            return
        }

        val channelPrefix = getChannelPrefix(channel)
        val colon = if (SkyHanniMod.feature.chat.playerColonHider) "" else ":"
        val levelFormat = getLevelFormat(name, level, levelColor)

        sendWithLink("$channelPrefix$elitePrefix$levelFormat§f$colon", event.message)
    }

    private fun sendWithLink(prefix: String, message: String) {
        val fullText = ChatComponentText(prefix)

        for (lines in message.split(" ")) {
            fullText.appendSibling(ChatComponentText(" "))
            if (patternUrl.matcher(lines).matches()) {
                val oneWord = ChatComponentText(lines)
                oneWord.chatStyle.chatClickEvent = ClickEvent(ClickEvent.Action.OPEN_URL, lines)
                fullText.appendSibling(oneWord)
            } else {
                fullText.appendSibling(ChatComponentText(lines))
            }
        }

        Minecraft.getMinecraft().thePlayer.addChatMessage(fullText)
    }

    private fun getLevelFormat(name: String, level: Int, levelColor: String): String {
        if (level == -1) return name

        return when (SkyHanniMod.feature.chat.skyblockLevelDesign) {
            0 -> "§8[§${levelColor}${level}§8] $name"
            1 -> "§${levelColor}§l${level} $name"
            2 -> "$name §8[§${levelColor}${level}§8]"
            3 -> name
            else -> "§8[§${levelColor}${level}§8] $name"
        }
    }

    private fun getChannelPrefix(channel: PlayerMessageChannel): String {
        if (channel == PlayerMessageChannel.ALL && !SkyHanniMod.feature.chat.allChannelPrefix) return ""

        val color = channel.prefixColor
        val small = channel.prefixSmall
        val large = channel.prefixLarge
        return when (SkyHanniMod.feature.chat.channelDesign) {
            0 -> "$color$large §8> "
            1 -> "$color$small> "
            2 -> "§8<$color$small§8> "
            3 -> "§8[$color$small§8] "
            4 -> "§8($color$small§8) "
            else -> "$color$large §8> "
        }
    }

    companion object {

        fun testAllChat() {
            val name = Minecraft.getMinecraft().thePlayer.name
            val message =
                "§6[⌬499] §8[§b123§8] §6[MVP§c++§6] $name§f: This is a all chat test message and will not be sent to hypixel."
            LorenzChatEvent(message, ChatComponentText(message)).postAndCatch()
        }

        fun testGuildChat() {
            val name = Minecraft.getMinecraft().thePlayer.name
            val message =
                "§2Guild > §6[MVP§f++§6] $name §2[GuildRank]§f: This is a guild chat test message and will not be sent to hypixel."
            LorenzChatEvent(message, ChatComponentText(message)).postAndCatch()
        }
    }
}