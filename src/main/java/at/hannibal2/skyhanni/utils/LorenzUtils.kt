package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MayorElection
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.mixins.transformers.AccessorGuiEditSign
import at.hannibal2.skyhanni.test.TestBingo
import at.hannibal2.skyhanni.utils.ChatUtils.lastButtonClicked
import at.hannibal2.skyhanni.utils.CollectionUtils.sortedDesc
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.NEUItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.StringUtils.capAtMinecraftLength
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.toDashlessUUID
import at.hannibal2.skyhanni.utils.renderables.Renderable
import com.google.gson.JsonPrimitive
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.launchwrapper.Launch
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.FMLCommonHandler
import java.io.Serializable
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Timer
import java.util.TimerTask
import java.util.regex.Matcher
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object LorenzUtils {

    val connectedToHypixel get() = HypixelData.hypixelLive || HypixelData.hypixelAlpha

    val onHypixel get() = connectedToHypixel && Minecraft.getMinecraft().thePlayer != null

    val isOnAlphaServer get() = onHypixel && HypixelData.hypixelAlpha

    val inSkyBlock get() = onHypixel && HypixelData.skyBlock

    val inHypixelLobby get() = onHypixel && HypixelData.inLobby

    val inDungeons get() = inSkyBlock && DungeonAPI.inDungeon()

    /**
     * Consider using IslandType.isInIsland() instead
     */
    val skyBlockIsland get() = HypixelData.skyBlockIsland

    val skyBlockArea get() = if (inSkyBlock) HypixelData.skyBlockArea else "?"

    val inKuudraFight get() = IslandType.KUUDRA_ARENA.isInIsland()

    val noTradeMode get() = HypixelData.noTrade

    val isStrandedProfile get() = inSkyBlock && HypixelData.stranded

    val isBingoProfile get() = inSkyBlock && (HypixelData.bingo || TestBingo.testBingo)

    val isIronmanProfile get() = inSkyBlock && HypixelData.ironman

    val lastWorldSwitch get() = HypixelData.joinedWorld

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
        val result = kotlin.math.round(this * multiplier) / multiplier
        val a = result.toString()
        val b = toString()
        return if (a.length > b.length) this else result
    }

    fun Float.round(decimals: Int): Float {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        val result = kotlin.math.round(this * multiplier) / multiplier
        val a = result.toString().length
        val b = toString().length
        return if (a > b) this else result.toFloat()
    }

    // TODO replace all calls with regex
    @Deprecated("Do not use complicated string operations", ReplaceWith("Regex"))
    fun String.between(start: String, end: String): String = this.split(start, end)[1]

    // TODO use derpy() on every use case
    val EntityLivingBase.baseMaxHealth: Int
        get() = this.getEntityAttribute(SharedMonsterAttributes.maxHealth).baseValue.toInt()

    fun formatPercentage(percentage: Double): String = formatPercentage(percentage, "0.00")

    fun formatPercentage(percentage: Double, format: String?): String =
        DecimalFormat(format).format(percentage * 100).replace(',', '.') + "%"

    @Deprecated("old code", ReplaceWith("i.addSeparators()"))
    fun formatInteger(i: Int): String = i.addSeparators()

    @Deprecated("old code", ReplaceWith("l.addSeparators()"))
    fun formatInteger(l: Long): String = l.addSeparators()

    @Deprecated("old code", ReplaceWith("d.round(round).addSeparators()"))
    fun formatDouble(d: Double, round: Int = 1): String {
        return d.round(round).addSeparators()
    }

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

    fun getSBMonthByName(month: String): Int {
        var monthNr = 0
        for (i in 1 .. 12) {
            val monthName = SkyBlockTime.monthName(i)
            if (month == monthName) {
                monthNr = i
            }
        }
        return monthNr
    }

    fun getPlayerUuid() = getRawPlayerUuid().toDashlessUUID()

    fun getRawPlayerUuid() = Minecraft.getMinecraft().thePlayer.uniqueID

    fun getPlayerName(): String = Minecraft.getMinecraft().thePlayer.name

    // (key -> value) -> (sorting value -> key item icon)
    fun fillTable(list: MutableList<List<Any>>, data: MutableMap<Pair<String, String>, Pair<Double, NEUInternalName>>) {
        val keys = data.mapValues { (_, v) -> v.first }.sortedDesc().keys
        val renderer = Minecraft.getMinecraft().fontRendererObj
        val longest = keys.map { it.first }.maxOfOrNull { renderer.getStringWidth(it.removeColor()) } ?: 0

        for (pair in keys) {
            val (name, second) = pair
            var displayName = name
            while (renderer.getStringWidth(displayName.removeColor()) < longest) {
                displayName += " "
            }

            data[pair]!!.second.getItemStackOrNull()?.let {
                list.add(listOf(it, "$displayName   $second"))
            }
        }
    }

    fun setTextIntoSign(text: String, line: Int = 0) {
        val gui = Minecraft.getMinecraft().currentScreen
        if (gui !is AccessorGuiEditSign) return
        gui.tileSign.signText[line] = ChatComponentText(text)
    }

    fun addTextIntoSign(addedText: String) {
        val gui = Minecraft.getMinecraft().currentScreen
        if (gui !is AccessorGuiEditSign) return
        val lines = gui.tileSign.signText
        val index = gui.editLine
        val text = lines[index].unformattedText + addedText
        lines[index] = ChatComponentText(text.capAtMinecraftLength(91))
    }

    fun colorCodeToRarity(colorCode: Char): String {
        return when (colorCode) {
            'f' -> "Common"
            'a' -> "Uncommon"
            '9' -> "Rare"
            '5' -> "Epic"
            '6' -> "Legendary"
            'd' -> "Mythic"
            'b' -> "Divine"
            '4' -> "Supreme" // legacy items
            else -> "Special"
        }
    }

    inline fun <reified T : Enum<T>> MutableList<List<Any>>.addSelector(
        prefix: String,
        getName: (T) -> String,
        isCurrent: (T) -> Boolean,
        crossinline onChange: (T) -> Unit,
    ) {
        add(buildSelector<T>(prefix, getName, isCurrent, onChange))
    }

    inline fun <reified T : Enum<T>> buildSelector(
        prefix: String,
        getName: (T) -> String,
        isCurrent: (T) -> Boolean,
        crossinline onChange: (T) -> Unit,
    ) = buildList {
        add(prefix)
        for (entry in enumValues<T>()) {
            val display = getName(entry)
            if (isCurrent(entry)) {
                add("§a[$display]")
            } else {
                add("§e[")
                add(Renderable.link("§e$display") {
                    onChange(entry)
                })
                add("§e]")
            }
            add(" ")
        }
    }

    inline fun MutableList<List<Any>>.addButton(
        prefix: String,
        getName: String,
        crossinline onChange: () -> Unit,
        tips: List<String> = emptyList(),
    ) {
        val onClick = {
            if ((System.currentTimeMillis() - lastButtonClicked) > 150) { // funny thing happen if I don't do that
                onChange()
                SoundUtils.playClickSound()
                lastButtonClicked = System.currentTimeMillis()
            }
        }
        add(buildList {
            add(prefix)
            add("§a[")
            if (tips.isEmpty()) {
                add(Renderable.link("§e$getName", false, onClick))
            } else {
                add(Renderable.clickAndHover("§e$getName", tips, false, onClick))
            }
            add("§a]")
        })
    }

    fun GuiEditSign.isRancherSign(): Boolean {
        if (this !is AccessorGuiEditSign) return false

        val tileSign = (this as AccessorGuiEditSign).tileSign
        return (tileSign.signText[1].unformattedText.removeColor() == "^^^^^^"
            && tileSign.signText[2].unformattedText.removeColor() == "Set your"
            && tileSign.signText[3].unformattedText.removeColor() == "speed cap!")
    }

    fun IslandType.isInIsland() = inSkyBlock && skyBlockIsland == this

    fun GuiContainerEvent.SlotClickEvent.makeShiftClick() {
        if (this.clickedButton == 1 && slot?.stack?.getItemCategoryOrNull() == ItemCategory.SACK) return
        slot?.slotNumber?.let { slotNumber ->
            Minecraft.getMinecraft().playerController.windowClick(
                container.windowId, slotNumber, 0, 1, Minecraft.getMinecraft().thePlayer
            )
            isCanceled = true
        }
    }

    private val recalculateDerpy =
        RecalculatingValue(1.seconds) { MayorElection.isPerkActive("Derpy", "DOUBLE MOBS HP!!!") }

    val isDerpy get() = recalculateDerpy.getValue()

    fun Int.derpy() = if (isDerpy) this / 2 else this

    fun Int.ignoreDerpy() = if (isDerpy) this * 2 else this

    fun runDelayed(duration: Duration, runnable: () -> Unit) {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                runnable()
            }
        }, duration.inWholeMilliseconds)
    }

    val JsonPrimitive.asIntOrNull get() = takeIf { it.isNumber }?.asInt

    fun sendTitle(text: String, duration: Duration, height: Double = 1.8, fontSize: Float = 4f) {
        TitleManager.sendTitle(text, duration, height, fontSize)
    }

    @Deprecated("Dont use this approach at all. check with regex or equals instead.", ReplaceWith("Regex or equals"))
    fun Iterable<String>.anyContains(element: String) = any { it.contains(element) }

    inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String): T? {
        val enums = enumValues<T>()
        return enums.firstOrNull { it.name == name }
    }

    inline fun <reified T : Enum<T>> enumValueOf(name: String) =
        enumValueOfOrNull<T>(name)
            ?: kotlin.error("Unknown enum constant for ${enumValues<T>().first().name.javaClass.simpleName}: '$name'")

    inline fun <reified T : Enum<T>> enumJoinToPattern(noinline transform: (T) -> CharSequence = { it.name }) =
        enumValues<T>().joinToString("|", transform = transform)

    fun isInDevEnviromen() = Launch.blackboard["fml.deobfuscatedEnvironment"] as Boolean

    fun shutdownMinecraft(reason: String? = null) {
        System.err.println("SkyHanni-${SkyHanniMod.version} forced the game to shutdown.")
        reason?.let {
            System.err.println("Reason: $it")
        }
        FMLCommonHandler.instance().handleExit(-1)
    }

    @Deprecated("moved", ReplaceWith("ChatUtils.sendCommandToServer(command)"))
    fun sendCommandToServer(command: String) {
        ChatUtils.sendCommandToServer(command)
    }

    /**
     * Get the group, otherwise, return null
     * @param groupName The group name in the pattern
     */
    fun Matcher.groupOrNull(groupName: String): String? {
        return runCatching { this.group(groupName) }.getOrNull()
    }

    @Deprecated("moved", ReplaceWith("ChatUtils.debug(message)"))
    fun debug(message: String) = ChatUtils.debug(message)

    @Deprecated("moved", ReplaceWith("ChatUtils.userError(message)"))
    fun userError(message: String) = ChatUtils.userError(message)

    @Deprecated("moved", ReplaceWith("ChatUtils.chat(message, prefix, prefixColor)"))
    fun chat(message: String, prefix: Boolean = true, prefixColor: String = "§e") =
        ChatUtils.chat(message, prefix, prefixColor)

    @Deprecated("moved", ReplaceWith("ChatUtils.clickableChat(message, command, prefix, prefixColor)"))
    fun clickableChat(message: String, command: String, prefix: Boolean = true, prefixColor: String = "§e") =
        ChatUtils.clickableChat(message, command, prefix, prefixColor)

    @Deprecated("moved", ReplaceWith("ChatUtils.hoverableChat(message, hover, command, prefix, prefixColor)"))
    fun hoverableChat(
        message: String,
        hover: List<String>,
        command: String? = null,
        prefix: Boolean = true,
        prefixColor: String = "§e",
    ) = ChatUtils.hoverableChat(message, hover, command, prefix, prefixColor)

    @Deprecated("moved", ReplaceWith("ChatUtils.sendMessageToServer(message)"))
    fun sendMessageToServer(message: String) = ChatUtils.sendMessageToServer(message)
}
