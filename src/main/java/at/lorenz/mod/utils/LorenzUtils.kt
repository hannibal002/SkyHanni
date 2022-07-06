package at.lorenz.mod.utils

import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText
import org.intellij.lang.annotations.Language
import java.text.SimpleDateFormat

class LorenzUtils {

    companion object {
        const val DEBUG_PREFIX = "[Debug] §7"

        fun debug(message: String) {
            internaChat(DEBUG_PREFIX + message)
        }

        fun warning(message: String) {
            internaChat("§cWarning! $message")
        }

        fun error(message: String) {
            internaChat("§4$message")
        }

        fun chat(message: String) {
            internaChat(message)
        }

        private fun internaChat(message: String) {
            val thePlayer = Minecraft.getMinecraft().thePlayer
            thePlayer.addChatMessage(ChatComponentText(message))
        }

        fun String.matchRegex(@Language("RegExp") regex: String): Boolean = regex.toRegex().matches(this)

        fun String.removeColorCodes(): String {
            val builder = StringBuilder()
            var skipNext = false
            for (c in this.toCharArray()) {
                if (c == '§') {
                    skipNext = true
                    continue
                }
                if (skipNext) {
                    skipNext = false
                    continue
                }
                builder.append(c)
            }

            return builder.toString()
        }

        fun SimpleDateFormat.formatCurrentTime(): String = this.format(System.currentTimeMillis())

        fun stripVanillaMessage(originalMessage: String): String {
            var message = originalMessage

            while (message.startsWith("§r")) {
                message = message.substring(2)
            }
            while (message.endsWith("§r")) {
                message = message.substring(0, message.length - 2)
            }

//        if (!message.startsWith(LorenzUtils.DEBUG_PREFIX + "chat api got (123)")) {
//            if (message.matchRegex("(.*)§r§7 \\((.{1,3})\\)")) {
//                val indexOf = message.lastIndexOf("(")
////                    LorenzAddons.testLogger.log("chat api got (123)!")
////                    LorenzAddons.testLogger.log("before: '$message'")
//                message = message.substring(0, indexOf - 5)
////                    LorenzAddons.testLogger.log("after: '$message'")
////                    LorenzAddons.testLogger.log("")
////                    LorenzUtils.debug("chat api got (123)")
////                } else if (message.endsWith("§r§7 (2)")) {
//////                    LorenzAddons.testLogger.log("other variant: '$message'")
//////                    LorenzAddons.testLogger.log("")
////                    LorenzUtils.debug("chat api got WRONG (123)")
//            }
//        }

            return message
        }

        fun Double.round(decimals: Int): Double {
            var multiplier = 1.0
            repeat(decimals) { multiplier *= 10 }
            return kotlin.math.round(this * multiplier) / multiplier
        }
    }
}