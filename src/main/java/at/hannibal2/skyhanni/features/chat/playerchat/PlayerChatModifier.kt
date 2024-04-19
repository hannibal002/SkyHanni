package at.hannibal2.skyhanni.features.chat.playerchat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.features.misc.MarkedPlayerManager
import at.hannibal2.skyhanni.utils.StringUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PlayerChatModifier {

    private val config get() = SkyHanniMod.feature.chat.playerMessage
    private val patterns = mutableListOf<Regex>()

    init {
        patterns.add("§[ab6]\\[(?:VIP|MVP)(?:§.|\\+)*] {1,2}(?:§[7ab6])?(\\w{2,16})".toRegex()) // ranked player with prefix everywhere
        patterns.add("§[7ab6](\\w{2,16})§r(?!§7x)(?!\$)".toRegex()) // all players without rank prefix in notification messages
    }

    @SubscribeEvent
    fun onChat(event: SystemMessageEvent) {
        val newMessage = cutMessage(event.chatComponent.formattedText)

        event.chatComponent = StringUtils.replaceIfNeeded(event.chatComponent, newMessage) ?: return
    }

    private fun cutMessage(input: String): String {
        var string = input

        if (config.playerRankHider) {
            for (pattern in patterns) {
                string = string.replace(pattern, "§b$1")
            }
            string = string.replace("§[7ab6]((?:\\w+){2,16})'s", "§b$1's")
            string = string.replace("§[7ab6]((?:\\w+){2,16}) (§.)", "§b$1 $2")
        }

        string = MarkedPlayerManager.replaceInChat(string)

        return string
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "chat.playerRankHider", "chat.playerMessage.playerRankHider")
        event.move(3, "chat.chatFilter", "chat.playerMessage.chatFilter")
    }
}
