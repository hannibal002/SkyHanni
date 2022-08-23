package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.HypixelData
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.util.ChatComponentText
import org.intellij.lang.annotations.Language
import java.text.DecimalFormat
import java.text.SimpleDateFormat

object LorenzUtils {

    val isOnHypixel: Boolean
        get() = HypixelData.hypixel && Minecraft.getMinecraft().thePlayer != null

    val inSkyblock: Boolean
        get() = isOnHypixel && HypixelData.skyblock

    val inDungeons: Boolean
        get() = inSkyblock && HypixelData.dungeon

    val skyBlockIsland: String
        get() = HypixelData.mode

    const val DEBUG_PREFIX = "[Debug] §7"
    private val log = LorenzLogger("chat/mod_sent")

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
        log.log(message)
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

    //TODO move into StringUtils
    fun String.matchRegex(@Language("RegExp") regex: String): Boolean = regex.toRegex().matches(this)


    fun SimpleDateFormat.formatCurrentTime(): String = this.format(System.currentTimeMillis())

    fun stripVanillaMessage(originalMessage: String): String {
        var message = originalMessage

        while (message.startsWith("§r")) {
            message = message.substring(2)
        }
        while (message.endsWith("§r")) {
            message = message.substring(0, message.length - 2)
        }
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

    fun formatPercentage(percentage: Double): String = formatPercentage(percentage, "0.00")

    fun formatPercentage(percentage: Double, format: String?): String =
        DecimalFormat(format).format(percentage * 100).replace(',', '.') + "%"

    fun formatInteger(i: Int): String = DecimalFormat("#,##0").format(i.toLong()).replace(',', '.')

    fun formatDouble(d: Double, format: String?): String =
        DecimalFormat(format).format(d).replace(',', 'x').replace('.', ',').replace('x', '.')

    fun formatDouble(d: Double): String = formatDouble(d, "#,##0.0")
}