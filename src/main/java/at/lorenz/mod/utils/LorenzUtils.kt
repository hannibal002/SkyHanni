package at.lorenz.mod.utils

import at.lorenz.mod.misc.HypixelData
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.util.ChatComponentText
import org.intellij.lang.annotations.Language
import java.text.SimpleDateFormat

object LorenzUtils {

    val isOnHypixel: Boolean
        get() = HypixelData.hypixel

    val inSkyblock: Boolean
        get() = HypixelData.hypixel && HypixelData.skyblock

    val inDungeons: Boolean
        get() = HypixelData.hypixel && HypixelData.skyblock && HypixelData.dungeon

    const val DEBUG_PREFIX = "[Debug] §7"

    fun debug(message: String) {
        internalChat(DEBUG_PREFIX + message)
    }

    fun warning(message: String) {
        internalChat("§cWarning! $message")
    }

    fun error(message: String) {
        internalChat("§4$message")
    }

    fun chat(message: String) {
        internalChat(message)
    }

    private fun internalChat(message: String) {
        val minecraft = Minecraft.getMinecraft()
        if (minecraft == null) {
            println(message)
            return
        }

        val thePlayer = minecraft.thePlayer
        if (thePlayer == null) {
            println(message)
            return
        }

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

    fun String.between(start: String, end: String): String = this.split(start, end)[1]

    val EntityLivingBase.baseMaxHealth: Double
        get() = this.getEntityAttribute(SharedMonsterAttributes.maxHealth).baseValue
}