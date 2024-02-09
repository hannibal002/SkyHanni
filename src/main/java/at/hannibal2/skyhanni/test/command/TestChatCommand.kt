package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.utils.ChatUtils
import net.minecraft.util.ChatComponentText
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.common.MinecraftForge

object TestChatCommand {

    fun command(args: Array<String>) {
        if (args.isEmpty()) {
            ChatUtils.userError("Specify a chat message to test!")
            return
        }

        val hidden = args.last() == "-s"
        var rawMessage = args.toList().joinToString(" ")
        if (!hidden) ChatUtils.chat("Testing message: §7$rawMessage", prefixColor = "§a")
        if (hidden) rawMessage = rawMessage.replace(" -s", "")
        val formattedMessage = rawMessage.replace("&", "§")
        ChatUtils.chat(formattedMessage, false)
        MinecraftForge.EVENT_BUS.post(ClientChatReceivedEvent(0, ChatComponentText(formattedMessage)))
    }
}
