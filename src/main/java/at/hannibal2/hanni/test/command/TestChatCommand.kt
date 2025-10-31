package at.hannibal2.hanni.test.command

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.commands.CommandCategory
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.OSUtils
import at.hannibal2.hanni.utils.StringUtils.stripHypixelMessage
import at.hannibal2.hanni.utils.chat.TextHelper.asComponent
import net.minecraft.util.IChatComponent

@HanniModule
object TestChatCommand {

    fun command(args: Array<String>) {
        if (args.isEmpty()) {
            val syntaxStrings = listOf(
                "§7Syntax: §e/shtestmessage §7<§echat message§7> [flags]",
                "   §7[-lines]§e: §7Split the message into multiple by newlines",
                "   §7[-complex]§e: §7Parse the message as a JSON chat component",
                "   §7[-clipboard]§e: §7Read the message from the clipboard",
                "   §7[-s]§e: §7Hide the testing message",
                "   §7[-sa]§e: §7Hide everything but the final message", // Not really sure why you'd want this
            )
            ChatUtils.userError("Specify a chat message to test!\n${syntaxStrings.joinToString("\n")}")
            return
        }

        HanniMod.launchCoroutine("test chat command") {
            val mutArgs = args.toMutableList()
            val multiLines = mutArgs.remove("-lines")
            val isComplex = mutArgs.remove("-complex")
            // cant use multi lines without clipboard
            val isClipboard = mutArgs.remove("-clipboard") || multiLines
            val isSilentAll = mutArgs.remove("-sa")
            val isSilent = mutArgs.remove("-s") || isSilentAll
            val text = if (isClipboard) {
                OSUtils.readFromClipboard() ?: return@launchCoroutine ChatUtils.userError("Clipboard does not contain a string!")
            } else mutArgs.joinToString(" ")
            if (multiLines) {
                for (line in text.split("\n")) {
                    extracted(isComplex, line, isSilent, isSilentAll)
                }
            } else {
                extracted(isComplex, text, isSilent, isSilentAll)
            }
        }
    }

    private fun extracted(isComplex: Boolean, text: String, isSilent: Boolean, isSilentAll: Boolean) {
        val component = if (isComplex) try {
            //#if TODO
            IChatComponent.Serializer.jsonToComponent(text) ?: "".asComponent()
            //#else
            //$$ "complex doesnt work on 1.21".asComponent()
            //#endif
        } catch (ex: Exception) {
            ChatUtils.userError("Please provide a valid JSON chat component (either in the command or via -clipboard)")
            return
        }
        else text.replace("&", "§").asComponent()

        println("component unformatted: ${component.unformattedText}")
        println("${component.unformattedTextForChat} ${component.chatStyle} ${component.siblings}")
        println(component)

        val rawText = component.formattedText.stripHypixelMessage().replace("§", "&").replace("\n", "\\n")
        if (!isSilent) ChatUtils.chat("Testing message: §7$rawText", prefixColor = "§a")

        test(component, isSilentAll)
    }

    private fun test(componentText: IChatComponent, isHidden: Boolean) {
        val message = componentText.formattedText.stripHypixelMessage()
        val event = HanniChatEvent(message, componentText)
        event.post()

        if (event.blockedReason != null) {
            if (!isHidden) ChatUtils.chat("§cChat blocked: ${event.blockedReason}")
            return
        }
        val finalMessage = event.chatComponent
        if (!isHidden && finalMessage.formattedText.stripHypixelMessage() != message) {
            ChatUtils.chat("§eChat modified!")
        }
        ChatUtils.chat(finalMessage)
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtestmessage") {
            description = "Sends a custom chat message client side in the chat"
            category = CommandCategory.DEVELOPER_TEST
            legacyCallbackArgs { command(it) }
        }
    }
}
