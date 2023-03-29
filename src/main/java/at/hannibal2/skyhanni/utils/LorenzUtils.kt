package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HyPixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.dungeon.DungeonData
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.toDashlessUUID
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.util.ChatComponentText
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object LorenzUtils {

    val isHyPixel: Boolean
        get() = HyPixelData.hypixel && Minecraft.getMinecraft().thePlayer != null

    val inSkyBlock: Boolean
        get() = isHyPixel && HyPixelData.skyBlock

    val inDungeons: Boolean
        get() = inSkyBlock && DungeonData.inDungeon()

    val skyBlockIsland: IslandType
        get() = HyPixelData.skyBlockIsland

    //TODO add cache
    val skyBlockArea: String
        get() = HyPixelData.readSkyBlockArea()

    val inKuudraFight: Boolean
        get() = skyBlockIsland == IslandType.KUUDRA_ARENA

    val noTradeMode: Boolean
        get() = HyPixelData.noTrade

    val isBingoProfile: Boolean
        get() = inSkyBlock && HyPixelData.bingo

    const val DEBUG_PREFIX = "[SkyHanni Debug] §7"
    private val log = LorenzLogger("chat/mod_sent")

    fun debug(message: String) {
        if (SkyHanniMod.feature.dev.debugEnabled) {
            if (internalChat(DEBUG_PREFIX + message)) {
                consoleLog("[Debug] $message")
            }
        } else {
            consoleLog("[Debug] $message")
        }
    }

    // TODO remove ig?
    fun warning(message: String) {
        internalChat("§cWarning! $message")
    }

    fun error(message: String) {
        internalChat("§c$message")
    }

    fun chat(message: String) {
        internalChat(message)
    }

    private fun internalChat(message: String): Boolean {
        log.log(message)
        val minecraft = Minecraft.getMinecraft()
        if (minecraft == null) {
            consoleLog(message.removeColor())
            return false
        }

        val thePlayer = minecraft.thePlayer
        if (thePlayer == null) {
            consoleLog(message.removeColor())
            return false
        }

        thePlayer.addChatMessage(ChatComponentText(message))
        return true
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
        return message
    }

    fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
    }

    // TODO replace all calls with regex
    fun String.between(start: String, end: String): String = this.split(start, end)[1]

    //TODO change to Int
    val EntityLivingBase.baseMaxHealth: Int
        get() = this.getEntityAttribute(SharedMonsterAttributes.maxHealth).baseValue.toInt()

    fun formatPercentage(percentage: Double): String = formatPercentage(percentage, "0.00")

    fun formatPercentage(percentage: Double, format: String?): String =
//        NumberFormat.getPercentInstance().format(percentage)
        DecimalFormat(format).format(percentage * 100).replace(',', '.') + "%"

    fun formatInteger(i: Int): String = formatInteger(i.toLong())

    fun formatInteger(l: Long): String = NumberFormat.getIntegerInstance().format(l)

    fun formatDouble(d: Double, round: Int = 1): String =
        NumberFormat.getNumberInstance().format(d.round(round))

    fun consoleLog(text: String) {
        SkyHanniMod.consoleLog(text)
    }

    fun getPointsForDojoRank(rank: String): Int {
        return when (rank) {
            "S" -> 1000
            "A" -> 800
            "B" -> 600
            "C" -> 400
            "D" -> 200
            "F" -> 0
            else -> 0
        }
    }

    fun <K, V : Comparable<V>> List<Pair<K, V>>.sorted(): List<Pair<K, V>> {
        return sortedBy { (_, value) -> value }
    }

    fun <K, V : Comparable<V>> Map<K, V>.sorted(): Map<K, V> {
        return toList().sorted().toMap()
    }

    fun <K, V : Comparable<V>> Map<K, V>.sortedDesc(): Map<K, V> {
        return toList().sorted().reversed().toMap()
    }

    fun getSBMonthByName(month: String): Int {
        var monthNr = 0
        for (i in 1..12) {
            val monthName = SkyBlockTime.monthName(i)
            if (month == monthName) {
                monthNr = i
            }
        }
        return monthNr
    }

    fun getPlayerUuid() = Minecraft.getMinecraft().thePlayer.uniqueID.toDashlessUUID()

    fun <E> MutableList<List<E>>.addAsSingletonList(text: E) {
        add(Collections.singletonList(text))
    }
}