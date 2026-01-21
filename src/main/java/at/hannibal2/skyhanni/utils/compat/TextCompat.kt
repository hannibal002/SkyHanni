package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.mixins.hooks.ComponentCreatedStore
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import net.minecraft.ChatFormatting
import net.minecraft.client.GuiMessageTag
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MessageSignature
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.network.chat.contents.PlainTextContents
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import java.net.URI
import kotlin.jvm.optionals.getOrNull
import kotlin.math.abs
import kotlin.time.Duration.Companion.minutes

private val unformattedTextCache = TimeLimitedCache<Component, String>(3.minutes)
private val formattedTextCache = TimeLimitedCache<TextCacheKey, String>(3.minutes)

private enum class FormattedTextSettings {
    DEFAULT,
    LESS_RESETS,
    LEADING_WHITE,
    LEADING_WHITE_LESS_RESETS,
    ;

    companion object {
        fun getByArgs(noExtraResets: Boolean, leadingWhite: Boolean): FormattedTextSettings {
            return when {
                noExtraResets && leadingWhite -> LEADING_WHITE_LESS_RESETS
                noExtraResets -> LESS_RESETS
                leadingWhite -> LEADING_WHITE
                else -> DEFAULT
            }
        }
    }
}

private data class TextCacheKey(val settings: FormattedTextSettings, val component: Component)

fun Component.unformattedTextForChatCompat(): String {
    return unformattedTextCache.getOrPut(this) {
        computeUnformattedTextCompat()
    }
}

private fun Component.computeUnformattedTextCompat(): String {
    if (this.contents is TranslatableContents) {
        return this.string
    }
    return (this.contents as? PlainTextContents)?.text().orEmpty()
}

fun Component.unformattedTextCompat(): String =
    iterator().map { it.unformattedTextForChatCompat() }.joinToString(separator = "")

// has to be a separate function for pattern mappings
fun Component?.formattedTextCompatLessResets(): String = this.formattedTextCompat(noExtraResets = true)
fun Component?.formattedTextCompatLeadingWhite(): String = this.formattedTextCompat(leadingWhite = true)
fun Component?.formattedTextCompatLeadingWhiteLessResets(): String =
    this.formattedTextCompat(noExtraResets = true, leadingWhite = true)

@JvmOverloads
@Suppress("unused")
fun Component?.formattedTextCompat(noExtraResets: Boolean = false, leadingWhite: Boolean = false): String {
    this ?: return ""
    val cacheKey = TextCacheKey(FormattedTextSettings.getByArgs(noExtraResets, leadingWhite), this)
    return formattedTextCache.getOrPut(cacheKey) {
        computeFormattedTextCompat(noExtraResets, leadingWhite)
    }
}

private fun Component?.computeFormattedTextCompat(noExtraResets: Boolean, leadingWhite: Boolean): String {
    this ?: return ""
    val sb = StringBuilder(50)
    var wasFormatted = false
    for (component in iterator()) {
        val chatStyle = component.style.chatStyle()
        if (chatStyle.isNotEmpty() && (leadingWhite || (wasFormatted && (sb.length != 2 || sb[0] != '§' || sb[1] != 'r')) || chatStyle != "§f")) {
            sb.append(chatStyle)
            wasFormatted = true
        }
        sb.append(component.unformattedTextForChatCompat())
        if (!noExtraResets) {
            sb.append("§r")
            wasFormatted = true
        } else if (component == Component.empty()) {
            sb.append("§r")
            wasFormatted = true
        }
    }
    return sb.removeSuffix("§r").removePrefix("§r").toString()
}

private val textColorLUT = ChatFormatting.entries
    .mapNotNull { formatting -> formatting.color?.let { it to formatting } }
    .toMap()

fun Style.chatStyle() = buildString {
    color?.let { append(it.toChatFormatting()?.toString() ?: "<${it.formatValue()}>") }
    if (isBold) append("§l")
    if (isItalic) append("§o")
    if (isUnderlined) append("§n")
    if (isStrikethrough) append("§m")
    if (isObfuscated) append("§k")
}

fun TextColor.toChatFormatting(): ChatFormatting? {
    return textColorLUT[this.value]
}

fun Component.iterator(): Sequence<Component> {
    return sequenceOf(this) + siblings.asSequence().flatMap { it.iterator() } // TODO: in theory we want to properly inherit styles here
}

fun MutableComponent.withColor(formatting: ChatFormatting): MutableComponent {
    return this.withStyle { it.withColor(formatting) }
}

fun MutableComponent.withColor(color: TextColor): MutableComponent {
    return this.withStyle { it.withColor(color) }
}

/**
 * This might have performance issues if you render it every frame idk
 */
fun MutableComponent.withColor(hex: String): MutableComponent {
    return this.withStyle { it.withColor(ColorUtils.getColorFromHex(hex)) }
}

fun createResourceLocation(domain: String, path: String): ResourceLocation {
    val textureLocation = ResourceLocation.fromNamespaceAndPath(domain, path)
    return textureLocation
}

fun createResourceLocation(path: String): ResourceLocation {
    val textureLocation = ResourceLocation.parse(path)
    return textureLocation
}

var Component.hover: Component?
    get() = this.style.hoverEvent?.takeIf {
        it.action() == HoverEvent.Action.SHOW_TEXT
    }?.let { (it as HoverEvent.ShowText).value }
    set(value) {
        value?.let { new -> this.copyIfNeeded().withStyle { it.withHoverEvent(HoverEvent.ShowText(new)) } }
    }

var Component.stackHover: ItemStack?
    get() = this.style.hoverEvent?.takeIf {
        it.action() == HoverEvent.Action.SHOW_ITEM
    }?.let { (it as HoverEvent.ShowItem).item }
    set(value) {
        value?.let { new -> this.copyIfNeeded().withStyle { it.withHoverEvent(HoverEvent.ShowItem(new)) } }
    }

var Component.command: String?
    get() = this.style.clickEvent?.takeIf {
        it.action() == ClickEvent.Action.RUN_COMMAND
    }?.let { (it as ClickEvent.RunCommand).command }
    set(value) {
        this.copyIfNeeded().withStyle { (it.withClickEvent(ClickEvent.RunCommand(value.orEmpty()))) }
    }

var Component.suggest: String?
    get() = this.style.clickEvent?.takeIf {
        it.action() == ClickEvent.Action.SUGGEST_COMMAND
    }?.let { (it as ClickEvent.SuggestCommand).command }
    set(value) {
        this.copyIfNeeded().withStyle { (it.withClickEvent(ClickEvent.SuggestCommand(value.orEmpty()))) }
    }

var Component.url: String?
    get() = this.style.clickEvent?.takeIf {
        it.action() == ClickEvent.Action.OPEN_URL
    }?.let { (it as ClickEvent.OpenUrl).uri.toString() }
    set(value) {
        this.copyIfNeeded().withStyle { (it.withClickEvent(ClickEvent.OpenUrl(URI.create(value.orEmpty())))) }
    }

var MutableComponent.underlined: Boolean
    get() = this.style.isUnderlined
    set(value) {
        this.withStyle { it.withUnderlined(value) }
    }

var MutableComponent.bold: Boolean
    get() = this.style.isBold
    set(value) {
        this.withStyle { it.withBold(value) }
    }

var MutableComponent.strikethrough: Boolean
    get() = this.style.isStrikethrough
    set(value) {
        this.withStyle { it.withStrikethrough(value) }
    }

var MutableComponent.italic: Boolean
    get() = this.style.isItalic
    set(value) {
        this.withStyle { it.withItalic(value) }
    }

var MutableComponent.obfuscated: Boolean
    get() = this.style.isObfuscated
    set(value) {
        this.withStyle { it.withObfuscated(value) }
    }

fun Style.setClickRunCommand(text: String): Style {
    return this.withClickEvent(ClickEvent.RunCommand(text))
}

fun Style.setHoverShowText(text: String): Style {
    return this.withHoverEvent(HoverEvent.ShowText(Component.literal(text)))
}

fun Style.setHoverShowText(text: Component): Style {
    return this.withHoverEvent(HoverEvent.ShowText(text))
}

fun addChatMessageToChat(message: Component, bypassSelfMessages: Boolean = false) {
    if (!bypassSelfMessages) (message as ComponentCreatedStore).`skyhanni$setCreated`()
    Minecraft.getInstance().player?.displayClientMessage(message, false)
}

fun addDeletableMessageToChat(component: Component, id: Int, bypassSelfMessages: Boolean = false) {
    if (!bypassSelfMessages) (component as ComponentCreatedStore).`skyhanni$setCreated`()
    Minecraft.getInstance().execute {
        Minecraft.getInstance().gui.chat.deleteMessage(idToMessageSignature(id))
        Minecraft.getInstance().gui.chat.addMessage(component, idToMessageSignature(id), GuiMessageTag.system())
    }
}

val map = mutableMapOf<Int, MessageSignature>()

fun idToMessageSignature(id: Int): MessageSignature {
    val newId = abs(id % (255 * 128))
    map[newId]?.let { return it }
    val bytes = ByteArray(256)
    val div = newId / 128
    val mod = newId % 128
    for (i in 0 until div) {
        bytes[i] = 127
    }
    bytes[div] = mod.toByte()
    return MessageSignature(bytes)
}

val defaultStyleConstructor: Style
    get() =
        Style.EMPTY

fun ClickEvent.value(): String {
    return when (this.action()) {
        ClickEvent.Action.OPEN_URL -> (this as ClickEvent.OpenUrl).uri.toString()
        ClickEvent.Action.RUN_COMMAND -> (this as ClickEvent.RunCommand).command
        ClickEvent.Action.SUGGEST_COMMAND -> (this as ClickEvent.SuggestCommand).command
        // we don't use these bottom 3 but might as well have them here
        ClickEvent.Action.CHANGE_PAGE -> (this as ClickEvent.ChangePage).page.toString()
        ClickEvent.Action.COPY_TO_CLIPBOARD -> (this as ClickEvent.CopyToClipboard).value
        ClickEvent.Action.OPEN_FILE -> (this as ClickEvent.OpenFile).path
        // todo use error manager here probably, not doing it now because it doesnt compile on 1.21
        else -> ""
    }

}

fun HoverEvent.value(): Component {
    return when (this.action()) {
        HoverEvent.Action.SHOW_TEXT -> (this as HoverEvent.ShowText).value
        HoverEvent.Action.SHOW_ITEM -> (this as HoverEvent.ShowItem).item.hoverName
        HoverEvent.Action.SHOW_ENTITY -> (this as HoverEvent.ShowEntity).entity.name.getOrNull() ?: Component.empty()
        else -> Component.empty()
    }
}

fun createHoverEvent(action: HoverEvent.Action?, component: MutableComponent): HoverEvent? {
    if (action == null) return null
    when (action) {
        HoverEvent.Action.SHOW_TEXT -> return HoverEvent.ShowText(component)
        // I really don't think anyone is using the other 2 lol
        else -> return null
    }
}

fun Component.changeColor(color: LorenzColor): Component =
    this.copyIfNeeded().withStyle(color.toChatFormatting())

fun Component.convertToJsonString(): String {
    //? < 1.21.6 {
    return Component.SerializerAdapter(net.minecraft.core.RegistryAccess.EMPTY).serialize(this, null, null).toString()
    //?} else {
    /*return net.minecraft.network.chat.ComponentSerialization.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, this).orThrow.toString()
    *///?}
}

fun Component.append(newText: Component): MutableComponent {
    return this.copyIfNeeded().append(newText)
}

val formattingPattern = Regex("§.(?:§.)?")

fun Component.append(newText: String): MutableComponent {
    val mutableText = this.copyIfNeeded()
    if (mutableText.string.matches(formattingPattern)) {
        return Component.literal(mutableText.string + newText)
    }
    return mutableText.append(newText)
}

fun MutableComponent.append(string: String = "", init: MutableComponent.() -> Unit): MutableComponent {
    return this.append(Component.literal(string).also(init))
}

fun MutableComponent.append(comp: Component, init: MutableComponent.() -> Unit): MutableComponent {
    return this.append(comp.copyIfNeeded().also(init))
}

fun MutableComponent.appendWithColor(string: String = "", color: Int, init: MutableComponent.() -> Unit = {}): MutableComponent {
    return this.append(Component.literal(string).withColor(color).also(init))
}

fun MutableComponent.appendWithColor(comp: Component, color: Int, init: MutableComponent.() -> Unit = {}): MutableComponent {
    return this.append(comp.copyIfNeeded().withColor(color).also(init))
}

fun MutableComponent.appendWithColor(string: String = "", color: ChatFormatting, init: MutableComponent.() -> Unit = {}): MutableComponent {
    return this.append(Component.literal(string).withColor(color).also(init))
}

fun MutableComponent.appendWithColor(comp: Component, color: ChatFormatting, init: MutableComponent.() -> Unit = {}): MutableComponent {
    return this.append(comp.copyIfNeeded().withColor(color).also(init))
}

fun MutableComponent.appendWithColor(string: String = "", color: TextColor, init: MutableComponent.() -> Unit = {}): MutableComponent {
    return this.append(Component.literal(string).withColor(color).also(init))
}

fun MutableComponent.appendWithColor(comp: Component, color: TextColor, init: MutableComponent.() -> Unit = {}): MutableComponent {
    return this.append(comp.copyIfNeeded().withColor(color).also(init))
}

fun List<Any>.mapToComponents(): List<Component> {
    val newList = mutableListOf<Component>()
    for (entry in this) {
        when (entry) {
            is String -> newList.add(Component.literal(entry))
            is Component -> newList.add(entry)
            else -> throw IllegalArgumentException("$entry is not String or Component")
        }
    }
    return newList
}

fun Component.replace(oldValue: String, newValue: String): Component {
    // this isnt perfect
    for (index in this.siblings.indices) {
        val sibling = this.siblings[index]
        this.siblings[index] = Component.literal(sibling.string.replace(oldValue, newValue)).withStyle(sibling.style)
    }
    return this
}

operator fun Component.plus(string: String): Component {
    return this.append(string)
}

fun componentBuilder(init: MutableComponent.() -> Unit): Component {
    return Component.empty().also(init)
}

fun Component.copyIfNeeded(): MutableComponent {
    if (this is MutableComponent) return this
    else return this.copy()
}
