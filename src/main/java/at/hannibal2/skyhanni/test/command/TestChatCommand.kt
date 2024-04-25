package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import kotlinx.coroutines.launch
import net.minecraft.util.ChatComponentText

object TestChatCommand {

    fun command(args: Array<String>) {
        if (args.isEmpty()) {
            ChatUtils.userError("Specify a chat message to test!")
            return
        }

        val last = args.last()
        if (last == "-clipboard") {
            SkyHanniMod.coroutineScope.launch {
                OSUtils.readFromClipboard()?.let {
                    test(it)
                } ?: run {
                    ChatUtils.userError("Clipboard does not contain a string!")
                }
            }
            return
        }
        val hidden = last == "-s"
        var rawMessage = args.toList().joinToString(" ")
        if (!hidden) ChatUtils.chat("Testing message: §7$rawMessage", prefixColor = "§a")
        if (hidden) rawMessage = rawMessage.replace(" -s", "")
        test(rawMessage.replace("&", "§"))
    }

    private fun test(message: String) {
        val event = LorenzChatEvent(message, ChatComponentText(message))
        event.postAndCatch()

        if (event.blockedReason != "") {
            ChatUtils.chat("§cChat blocked: ${event.blockedReason}")
        } else {
            val finalMessage = event.chatComponent.formattedText
            if (LorenzUtils.stripVanillaMessage(finalMessage) != LorenzUtils.stripVanillaMessage(message)) {
                ChatUtils.chat("§eChat modified!")
            }
            ChatUtils.chat(finalMessage, false)
        }
    }
}
