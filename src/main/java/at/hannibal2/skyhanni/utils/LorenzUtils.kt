package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MayorElection
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.mixins.transformers.AccessorGuiEditSign
import at.hannibal2.skyhanni.test.TestBingo
import at.hannibal2.skyhanni.utils.NEUItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.StringUtils.capAtMinecraftLength
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.toDashlessUUID
import at.hannibal2.skyhanni.utils.renderables.Renderable
import com.google.gson.JsonPrimitive
import io.github.moulberry.moulconfig.observer.Observer
import io.github.moulberry.moulconfig.observer.Property
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import java.awt.Color
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.Timer
import java.util.TimerTask
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object LorenzUtils {

    val onHypixel get() = (HypixelData.hypixelLive || HypixelData.hypixelAlpha) && Minecraft.getMinecraft().thePlayer != null

    val isOnAlphaServer get() = onHypixel && HypixelData.hypixelAlpha

    val inSkyBlock get() = onHypixel && HypixelData.skyBlock

    val inDungeons get() = inSkyBlock && DungeonAPI.inDungeon()

    val skyBlockIsland get() = HypixelData.skyBlockIsland

    val skyBlockArea get() = if (inSkyBlock) HypixelData.skyBlockArea else "?"

    val inKuudraFight get() = skyBlockIsland == IslandType.KUUDRA_ARENA

    val noTradeMode get() = HypixelData.noTrade

    val isStrandedProfile get() = HypixelData.stranded

    val isBingoProfile get() = inSkyBlock && (HypixelData.bingo || TestBingo.testBingo)

    val lastWorldSwitch get() = HypixelData.joinedWorld

    const val DEBUG_PREFIX = "[SkyHanni Debug] §7"
    private val log = LorenzLogger("chat/mod_sent")
    var lastButtonClicked = 0L

    fun debug(message: String) {
        if (SkyHanniMod.feature.dev.debug.enabled && internalChat(DEBUG_PREFIX + message)) {
            consoleLog("[Debug] $message")
        }
    }

    // TODO remove ig?
    fun warning(message: String) {
        internalChat("§cWarning! $message")
    }

    fun error(message: String) {
        println("error: '$message'")
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
        val result = kotlin.math.round(this * multiplier) / multiplier
        val a = result.toString()
        val b = toString()
        return if (a.length > b.length) this else result
    }

    fun Float.round(decimals: Int): Float {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        val result = kotlin.math.round(this * multiplier) / multiplier
        val a = result.toString()
        val b = toString()
        return if (a.length > b.length) this else result.toFloat()
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

    fun formatInteger(i: Int): String = formatInteger(i.toLong())

    fun formatInteger(l: Long): String = NumberFormat.getIntegerInstance().format(l)

    fun formatDouble(d: Double, round: Int = 1): String {
        val numberInstance = NumberFormat.getNumberInstance()
        numberInstance.maximumFractionDigits = round
        return numberInstance.format(d.round(round))
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

    fun getPlayerUuid() = getRawPlayerUuid().toDashlessUUID()

    fun getRawPlayerUuid() = Minecraft.getMinecraft().thePlayer.uniqueID

    fun getPlayerName(): String = Minecraft.getMinecraft().thePlayer.name

    fun <E> MutableList<List<E>>.addAsSingletonList(text: E) {
        add(Collections.singletonList(text))
    }

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

    fun setTextIntoSign(text: String) {
        val gui = Minecraft.getMinecraft().currentScreen
        if (gui !is AccessorGuiEditSign) return
        gui.tileSign.signText[0] = ChatComponentText(text)
    }

    fun addTextIntoSign(addedText: String) {
        val gui = Minecraft.getMinecraft().currentScreen
        if (gui !is AccessorGuiEditSign) return
        val lines = gui.tileSign.signText
        val index = gui.editLine
        val text = lines[index].unformattedText + addedText
        lines[index] = ChatComponentText(text.capAtMinecraftLength(90))
    }

    fun clickableChat(message: String, command: String) {
        val text = ChatComponentText(message)
        val fullCommand = "/" + command.removePrefix("/")
        text.chatStyle.chatClickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, fullCommand)
        text.chatStyle.chatHoverEvent =
            HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText("§eExecute $fullCommand"))
        Minecraft.getMinecraft().thePlayer.addChatMessage(text)
    }

    fun hoverableChat(message: String, hover: List<String>, command: String? = null) {
        val text = ChatComponentText(message)
        text.chatStyle.chatHoverEvent =
            HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(hover.joinToString("\n")))

        if (command != null) {
            text.chatStyle.chatClickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/${command.removePrefix("/")}")
        }

        Minecraft.getMinecraft().thePlayer.addChatMessage(text)
    }

    fun <K, V> Map<K, V>.moveEntryToTop(matcher: (Map.Entry<K, V>) -> Boolean): Map<K, V> {
        val entry = entries.find(matcher)
        if (entry != null) {
            val newMap = linkedMapOf(entry.key to entry.value)
            newMap.putAll(this)
            return newMap
        }
        return this
    }

    private var lastMessageSent = 0L

    fun sendCommandToServer(command: String) {
        sendMessageToServer("/$command")
    }

    fun sendMessageToServer(message: String) {
        if (System.currentTimeMillis() > lastMessageSent + 2_000) {
            lastMessageSent = System.currentTimeMillis()
            val thePlayer = Minecraft.getMinecraft().thePlayer
            thePlayer.sendChatMessage(message)
        }
    }

    // MoulConfig is in Java, I don't want to downgrade this logic
    fun <T> onChange(vararg properties: Property<out T>, observer: Observer<T>) {
        for (property in properties) {
            property.whenChanged { a, b -> observer.observeChange(a, b) }
        }
    }

    fun <T> onToggle(vararg properties: Property<out T>, observer: Runnable) {
        onChange(*properties) { _, _ -> observer.run() }
    }

    fun <T> Property<out T>.onToggle(observer: Runnable) {
        whenChanged { _, _ -> observer.run() }
    }

    fun <T> Property<out T>.afterChange(observer: T.() -> Unit) {
        whenChanged { _, new -> observer(new) }
    }

    fun <K, V> Map<K, V>.editCopy(function: MutableMap<K, V>.() -> Unit) = toMutableMap().also { function(it) }.toMap()

    fun <T> List<T>.editCopy(function: MutableList<T>.() -> Unit) = toMutableList().also { function(it) }.toList()

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

    // TODO nea?
//    fun <T> dynamic(block: () -> KMutableProperty0<T>?): ReadWriteProperty<Any?, T?> {
//        return object : ReadWriteProperty<Any?, T?> {
//            override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
//                return block()?.get()
//            }
//
//            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
//                if (value != null)
//                    block()?.set(value)
//            }
//        }
//    }

    fun <T, R> dynamic(root: KProperty0<R?>, child: KMutableProperty1<R, T>) = object : ReadWriteProperty<Any?, T?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
            val rootObj = root.get() ?: return null
            return child.get(rootObj)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
            if (value == null) return
            val rootObj = root.get() ?: return
            child.set(rootObj, value)
        }
    }

    fun List<String>.nextAfter(after: String, skip: Int = 1): String? {
        var missing = -1
        for (line in this) {
            if (line == after) {
                missing = skip - 1
                continue
            }
            if (missing == 0) {
                return line
            }
            if (missing != -1) {
                missing--
            }
        }
        return null
    }

    fun GuiEditSign.isRancherSign(): Boolean {
        if (this !is AccessorGuiEditSign) return false

        val tileSign = (this as AccessorGuiEditSign).tileSign
        return (tileSign.signText[1].unformattedText.removeColor() == "^^^^^^" && tileSign.signText[2].unformattedText.removeColor() == "Set your" && tileSign.signText[3].unformattedText.removeColor() == "speed cap!")
    }

    fun IslandType.isInIsland() = inSkyBlock && (skyBlockIsland == this || this == IslandType.CATACOMBS && inDungeons)

    fun <K> MutableMap<K, Int>.addOrPut(key: K, number: Int): Int {
        val currentValue = this[key] ?: 0
        val newValue = currentValue + number
        this[key] = newValue
        return newValue
    }

    fun <K> MutableMap<K, Long>.addOrPut(key: K, number: Long): Long {
        val currentValue = this[key] ?: 0L
        val newValue = currentValue + number
        this[key] = newValue
        return newValue
    }

    fun <K> MutableMap<K, Double>.addOrPut(key: K, number: Double): Double {
        val currentValue = this[key] ?: 0.0
        val newValue = currentValue + number
        this[key] = newValue
        return newValue
    }

    fun <K, N : Number> MutableMap<K, N>.sumAllValues(): Double {
        if (values.isEmpty()) return 0.0

        return when (values.first()) {
            is Double -> values.sumOf { it.toDouble() }
            is Float -> values.sumOf { it.toDouble() }
            is Long -> values.sumOf { it.toLong() }.toDouble()
            else -> values.sumOf { it.toInt() }.toDouble()
        }
    }

    /** transfer string colors from the config to java.awt.Color */
    fun String.toChromaColor() = Color(SpecialColour.specialToChromaRGB(this), true)

    fun <E> List<E>.getOrNull(index: Int): E? {
        return if (index in indices) {
            get(index)
        } else null
    }

    fun <T : Any> T?.toSingletonListOrEmpty(): List<T> {
        if (this == null) return emptyList()
        return listOf(this)
    }

    fun Field.makeAccessible() = also { isAccessible = true }

    // Taken and modified from Skytils
    @JvmStatic
    fun <T> T.equalsOneOf(vararg other: T): Boolean {
        for (obj in other) {
            if (this == obj) return true
        }
        return false
    }

    infix fun <K, V> MutableMap<K, V>.put(pairs: Pair<K, V>) {
        this[pairs.first] = pairs.second
    }

    fun Field.removeFinal(): Field {
        javaClass.getDeclaredField("modifiers").makeAccessible().set(this, modifiers and (Modifier.FINAL.inv()))
        return this
    }

    fun <T> List<T>.indexOfFirst(vararg args: T) = args.map { indexOf(it) }.firstOrNull { it != -1 }

    private val recalculateDerpy =
        RecalculatingValue(1.seconds) { MayorElection.isPerkActive("Derpy", "DOUBLE MOBS HP!!!") }

    val isDerpy get() = recalculateDerpy.getValue()

    fun Int.derpy() = if (isDerpy) this / 2 else this

    fun runDelayed(duration: Duration, runnable: () -> Unit) {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                runnable()
            }
        }, duration.inWholeMilliseconds)
    }

    val JsonPrimitive.asIntOrNull get() = takeIf { it.isNumber }?.asInt

    fun <T> T.transformIf(condition: T.() -> Boolean, transofmration: T.() -> T) =
        if (condition()) transofmration(this) else this

    fun <T> T.conditionalTransform(condition: Boolean, ifTrue: T.() -> Any, ifFalse: T.() -> Any) =
        if (condition) ifTrue(this) else ifFalse(this)

    fun sendTitle(text: String, duration: Duration, height: Double = 1.8) {
        TitleManager.sendTitle(text, duration, height)
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

    operator fun MatchResult?.get(index: Int) = this?.groupValues?.get(index)

    fun <T> Sequence<T>.takeWhileInclusive(predicate: (T) -> Boolean) = sequence {
        with(iterator()) {
            while (hasNext()) {
                val next = next()
                yield(next)
                if (!predicate(next)) break
            }
        }
    }
}
