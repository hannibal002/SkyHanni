package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import kotlinx.coroutines.launch
import net.minecraft.util.ChatComponentText
import net.minecraft.util.IChatComponent

object TestChatCommand {

    fun command(args: Array<String>) {
        if (args.isEmpty()) {
            ChatUtils.userError("Specify a chat message to test!")
            return
        }

        SkyHanniMod.coroutineScope.launch {
            val mutArgs = args.toMutableList()
            val isComplex = mutArgs.remove("-complex")
            val isClipboard = mutArgs.remove("-clipboard")
            val isHidden = mutArgs.remove("-s")
            val text = if (isClipboard) {
                OSUtils.readFromClipboard() ?: return@launch ChatUtils.userError("Clipboard does not contain a string!")
            } else mutArgs.joinToString(" ")
            val component =
                if (isComplex) IChatComponent.Serializer.jsonToComponent(text)
                else ChatComponentText(text.replace("&", "§"))
            if (!isHidden) ChatUtils.chat("Testing message: §7${component.formattedText}", prefixColor = "§a")
            test(component)
        }
    }

    private fun test(componentText: IChatComponent) {
        val event = LorenzChatEvent(LorenzUtils.stripVanillaMessage(componentText.formattedText), componentText)
        event.postAndCatch()

        if (event.blockedReason != "") {
            ChatUtils.chat("§cChat blocked: ${event.blockedReason}")
        } else {
            val finalMessage = event.chatComponent
            if (LorenzUtils.stripVanillaMessage(finalMessage.formattedText) != LorenzUtils.stripVanillaMessage(componentText.formattedText)) {
                ChatUtils.chat("§eChat modified!")
            }
            ChatUtils.chatComponent(finalMessage)
        }
    }
}
