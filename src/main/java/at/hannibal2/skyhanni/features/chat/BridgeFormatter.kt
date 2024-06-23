package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.hypixel.chat.event.GuildChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object BridgeFormatter {

    private val config get() = SkyHanniMod.feature.chat.bridgeFormatter

    @SubscribeEvent
    fun onGuildChat(event: GuildChatEvent) {
        if (!config.enabled) return

        val message = event.messageString
        val player = event.authorString
        if (!player.contains(config.bridgeName.spaceless())) return

        val sep = config.separator.spaceless()
        val pattern = """^\s*(\S+)\s*${Regex.escape(sep)}\s+(.*)$""".toRegex()
        val matchRes = pattern.matchEntire(message) ?: return
        val (ign, msg) = matchRes.destructured

        // TODO: Optionally: replaces "X replying to Y: msg" with "Y <- X: msg" -> new config for this?

        val format = config.bridgeFormat
        val toSend = format
            .replace("&", "§")
            .replace("%ign%", ign)
            .replace("%msg%", msg)

        event.chatComponent = ChatComponentText(toSend)
    }

    private fun String.spaceless() = replace(" ", "")

    fun testFormat() {
        val toSend = config.bridgeFormat
            .replace("&", "§")
            .replace("%ign%", "DiscordName")
            .replace("%msg%", "DiscordMessage")
        ChatUtils.chat(toSend, prefix = false)
    }
}
