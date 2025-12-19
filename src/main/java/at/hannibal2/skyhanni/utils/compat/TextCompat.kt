package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.LorenzColor
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
//#if MC < 1.16
//$$ import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
//$$ import net.minecraft.util.ChatComponentText
//#endif
//#if MC > 1.16
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.TextColor
import kotlin.time.Duration.Companion.minutes
//#endif
//#if MC > 1.21
import net.minecraft.network.chat.contents.PlainTextContents
import net.minecraft.client.GuiMessageTag
import net.minecraft.network.chat.MessageSignature
import java.net.URI
import kotlin.jvm.optionals.getOrNull
import kotlin.math.abs
import net.minecraft.network.chat.contents.TranslatableContents
//#endif
//#if MC > 1.16
private val unformattedTextCache = TimeLimitedCache<Component, String>(3.minutes)
private val formattedTextCache = TimeLimitedCache<TextCacheKey, String>(3.minutes)

private enum class FormattedTextSettings(noExtraResets: Boolean, leadingWhite: Boolean) {
    DEFAULT(false, false),
    LESS_RESETS(true, false),
    LEADING_WHITE(false, true),
    LEADING_WHITE_LESS_RESETS(true, true),
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
//#endif

fun Component.unformattedTextForChatCompat(): String {
//#if MC < 1.16
//$$     return this.unformattedTextForChat
//#elseif MC < 1.21
//$$ return this.asString()
//#else
    return unformattedTextCache.getOrPut(this) {
        computeUnformattedTextCompat()
    }
}

private fun Component.computeUnformattedTextCompat(): String {
    if (this.contents is TranslatableContents) {
        return this.string
    }
    return (this.contents as? PlainTextContents)?.text().orEmpty()
//#endif
}

fun Component.unformattedTextCompat(): String =
//#if MC < 1.16
//$$     this.unformattedText
//#else
iterator().map { it.unformattedTextForChatCompat() }.joinToString(separator = "")
//#endif

// has to be a separate function for pattern mappings
fun Component?.formattedTextCompatLessResets(): String = this.formattedTextCompat(noExtraResets = true)
fun Component?.formattedTextCompatLeadingWhite(): String = this.formattedTextCompat(leadingWhite = true)
fun Component?.formattedTextCompatLeadingWhiteLessResets(): String =
    this.formattedTextCompat(noExtraResets = true, leadingWhite = true)

@JvmOverloads
@Suppress("unused")
fun Component?.formattedTextCompat(noExtraResets: Boolean = false, leadingWhite: Boolean = false): String {
//#if MC < 1.16
//$$     return this?.formattedText.orEmpty()
//$$ }
//#else
    this ?: return ""
    val cacheKey = TextCacheKey(FormattedTextSettings.getByArgs(noExtraResets, leadingWhite), this)
    return formattedTextCache.getOrPut(cacheKey) {
        computeFormattedTextCompat(noExtraResets, leadingWhite)
    }
}

private fun Component?.computeFormattedTextCompat(noExtraResets: Boolean, leadingWhite: Boolean): String {
    this ?: return ""
    val sb = StringBuilder(50)
    var wasFormatted  = false
    for (component in iterator()) {
        val chatStyle = component.style.chatStyle()
        if (chatStyle.isNotEmpty() && (leadingWhite || (wasFormatted && (sb.length != 2 || sb.get(0) != '§' || sb.get(1) != 'r')) || chatStyle != "§f")) {
            sb.append(chatStyle)
            wasFormatted  = true
        }
        sb.append(component.unformattedTextForChatCompat())
        if (!noExtraResets) {
            sb.append("§r")
            wasFormatted  = true
        } else if (component == Component.empty()){
            sb.append("§r")
            wasFormatted  = true
        }
    }
    return sb.removeSuffix("§r").removePrefix("§r").toString()
}

private val textColorLUT = ChatFormatting.entries
    .mapNotNull { formatting -> formatting.color?.let { it to formatting } }
    .toMap()

fun Style.chatStyle() = buildString {
    color?.let { append(it.toChatFormatting()?.toString() ?: "§r") }
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
//#endif

//#if MC > 1.21
fun MutableComponent.withColor(formatting: ChatFormatting): Component {
    return this.withStyle { it.withColor(formatting) }
}
//#endif

fun createResourceLocation(domain: String, path: String): ResourceLocation {
    //#if MC < 1.21
    //$$ val textureLocation = Identifier(domain, path)
    //#else
    val textureLocation = ResourceLocation.fromNamespaceAndPath(domain, path)
    //#endif
    return textureLocation
}

fun createResourceLocation(path: String): ResourceLocation {
    //#if MC < 1.21
    //$$ val textureLocation = Identifier(path)
    //#else
    val textureLocation = ResourceLocation.parse(path)
    //#endif
    return textureLocation
}

var Component.hover: Component?
    //#if MC < 1.16
    //$$ get() = this.chatStyle.chatHoverEvent?.let { if (it.action == HoverEvent.Action.SHOW_TEXT) it.value else null }
    //#else
    get() = this.style.hoverEvent?.let { if (it.action() == HoverEvent.Action.SHOW_TEXT) (it as HoverEvent.ShowText).value else null }
    //#endif
    set(value) {
        //#if MC < 1.16
        //$$ this.chatStyle.chatHoverEvent = value?.let { HoverEvent(HoverEvent.Action.SHOW_TEXT, it) }
        //#else
        value?.let { value -> (this as MutableComponent).withStyle { it.withHoverEvent(HoverEvent.ShowText(value)) } }
        //#endif
    }

var Component.command: String?
    //#if MC < 1.21
    //$$ get() = this.style.clickEvent?.let { if (it.action == ClickEvent.Action.RUN_COMMAND) it.value else null }
    //#else
    get() = this.style.clickEvent?.let { if (it.action() == ClickEvent.Action.RUN_COMMAND) (it as ClickEvent.RunCommand).command else null }
    //#endif
    set(value) {
        //#if MC < 1.16
        //$$ this.chatStyle.chatClickEvent = value?.let { ClickEvent(ClickEvent.Action.RUN_COMMAND, it) }
        //#else
        (this as MutableComponent).withStyle { (it.withClickEvent(ClickEvent.RunCommand(value.orEmpty()))) }
        //#endif
    }

var Component.suggest: String?
    //#if MC < 1.21
    //$$ get() = this.style.clickEvent?.let { if (it.action == ClickEvent.Action.SUGGEST_COMMAND) it.value else null }
    //#else
    get() = this.style.clickEvent?.let { if (it.action() == ClickEvent.Action.SUGGEST_COMMAND) (it as ClickEvent.SuggestCommand).command else null }
    //#endif
    set(value) {
        //#if MC < 1.16
        //$$ this.chatStyle.chatClickEvent = value?.let { ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, it) }
        //#else
        (this as MutableComponent).withStyle { (it.withClickEvent(ClickEvent.SuggestCommand(value.orEmpty()))) }
        //#endif
    }

var Component.url: String?
    //#if MC < 1.21
    //$$ get() = this.style.clickEvent?.let { if (it.action == ClickEvent.Action.OPEN_URL) it.value else null }
    //#else
    get() = this.style.clickEvent?.let { if (it.action() == ClickEvent.Action.OPEN_URL) (it as ClickEvent.OpenUrl).uri.toString() else null }
    //#endif
    set(value) {
        //#if MC < 1.16
        //$$ this.chatStyle.chatClickEvent = value?.let { ClickEvent(ClickEvent.Action.OPEN_URL, it) }
        //#else
        (this as MutableComponent).withStyle { (it.withClickEvent(ClickEvent.OpenUrl(URI.create(value.orEmpty())))) }
        //#endif
    }

fun Style.setClickRunCommand(text: String): Style {
    //#if MC < 1.21
    //$$ return this.setChatClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, text))
    //#else
    return this.withClickEvent(ClickEvent.RunCommand(text))
    //#endif
}

fun Style.setHoverShowText(text: String): Style {
    //#if MC < 1.21
    //$$ return this.setChatHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, text.asComponent()))
    //#else
    return this.withHoverEvent(HoverEvent.ShowText(Component.nullToEmpty(text)))
    //#endif
}

fun Style.setHoverShowText(text: Component): Style {
    //#if MC < 1.21
    //$$ return this.setChatHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, text))
    //#else
    return this.withHoverEvent(HoverEvent.ShowText(text))
    //#endif
}

fun Component.appendString(text: String): Component =
    //#if MC < 1.16
    //$$ this.appendText(text)
//#else
(this as MutableComponent).append(text)
//#endif

fun Component.appendComponent(component: Component): Component =
    //#if MC < 1.16
    //$$ this.appendSibling(component)
//#else
(this as MutableComponent).append(component)
//#endif

fun addChatMessageToChat(message: Component) {
    //#if FORGE
    //$$ Minecraft.getInstance().player.addChatMessage(message)
    //#else
    Minecraft.getInstance().player?.displayClientMessage(message, false)
    //#endif
}

fun addDeletableMessageToChat(component: Component, id: Int) {
    //#if MC < 1.16
    //$$ Minecraft.getMinecraft().ingameGUI.chatGUI.printChatMessageWithOptionalDeletion(component, id)
    //#else
    Minecraft.getInstance().execute {
       Minecraft.getInstance().gui.chat.deleteMessage(idToMessageSignature(id))
       Minecraft.getInstance().gui.chat.addMessage(component, idToMessageSignature(id), GuiMessageTag.system())
    }
    //#endif
}

//#if MC > 1.21
val map = mutableMapOf<Int, MessageSignature>()

fun idToMessageSignature(id: Int): MessageSignature {
    val newId = abs(id % (255*128))
    if (map.contains(newId)) return map[newId]!!
    val bytes = ByteArray(256)
    val div = newId / 128
    val mod = newId % 128
    for (i in 0 until div) {
        bytes[i] = 127
    }
    bytes[div] = mod.toByte()
    return MessageSignature(bytes)
}
//#endif

val defaultStyleConstructor: Style get() =
    //#if MC < 1.16
    //$$ ChatStyle()
//#else
Style.EMPTY
//#endif

fun ClickEvent.value(): String {
    //#if MC < 1.21
    //$$ return this.value
    //#else
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
    //#endif

}

fun HoverEvent.value(): Component {
    //#if MC < 1.21
    //$$ return this.contents
    //#else
    return when (this.action()) {
        HoverEvent.Action.SHOW_TEXT -> (this as HoverEvent.ShowText).value
        HoverEvent.Action.SHOW_ITEM -> (this as HoverEvent.ShowItem).item.hoverName
        HoverEvent.Action.SHOW_ENTITY -> (this as HoverEvent.ShowEntity).entity.name.getOrNull() ?: Component.empty()
        else -> Component.empty()
    }
    //#endif
}

//#if MC < 1.21
//$$ fun createHoverEvent(action: HoverEvent.Action?, component: TextComponent): HoverEvent? {
//$$     if (action == null) return null
//$$     return HoverEvent(action, component)
//$$ }
//#else
fun createHoverEvent(action: HoverEvent.Action?, component: MutableComponent): HoverEvent? {
    if (action == null) return null
    when (action) {
        HoverEvent.Action.SHOW_TEXT -> return HoverEvent.ShowText(component)
        // I really don't think anyone is using the other 2 lol
        else -> return null
    }
}
//#endif

fun Component.changeColor(color: LorenzColor): Component =
    //#if MC < 1.21
    //$$ this.shallowCopy().setStyle(this.style.withColor(color.toChatFormatting()))
//#else
this.copy().withStyle(color.toChatFormatting())
//#endif

fun Component.convertToJsonString(): String {
    //#if MC < 1.21
    //$$ return Text.Serializer.componentToJson(this)
    //#elseif MC < 1.21.6
    return Component.SerializerAdapter(net.minecraft.core.RegistryAccess.EMPTY).serialize(this, null, null).toString()
    //#else
    //$$ return net.minecraft.network.chat.ComponentSerialization.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, this).orThrow.toString()
    //#endif
}

//#if MC > 1.21
fun Component.append(newText: Component): Component {
    return (this as MutableComponent).append(newText)
}

val formattingPattern = Regex("§.(?:§.)?")

fun Component.append(newText: String): Component {
    val mutableText = this as MutableComponent
    if (mutableText.string.matches(formattingPattern)) {
        return Component.nullToEmpty(mutableText.string + newText)
    }
    return mutableText.append(newText)
}
//#else
//$$ fun net.minecraft.text.Text.append(string: String): net.minecraft.text.Text {
//$$     return at.hannibal2.skyhanni.utils.compat.Text.of(this.text + string)
//$$ }
//$$
//$$ fun net.minecraft.text.Text.append(newText: Text): net.minecraft.text.Text {
//$$     return at.hannibal2.skyhanni.utils.compat.Text.of(this.text + newText.text)
//$$ }
//#endif
