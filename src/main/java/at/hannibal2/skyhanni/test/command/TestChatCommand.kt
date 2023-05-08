package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.util.ChatComponentText
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.common.MinecraftForge

object TestChatCommand {
    fun command(args: Array<String>) {
        if (args.isEmpty()) {
            LorenzUtils.chat("§c[SkyHanni] Specify a chat message to test")
            return
        }
        val rawMessage = args.toList().joinToString(" ")
        LorenzUtils.chat("§a[SkyHanni] testing message: §7$rawMessage")
        val formattedMessage = rawMessage.replace("&", "§")
        LorenzUtils.chat(formattedMessage)
        MinecraftForge.EVENT_BUS.post(ClientChatReceivedEvent(0, ChatComponentText(formattedMessage)))
    }
}