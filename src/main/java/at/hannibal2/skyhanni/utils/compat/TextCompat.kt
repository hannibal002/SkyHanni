package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.LorenzColor
import net.minecraft.client.MinecraftClient
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Identifier
//#if MC < 1.16
//$$ import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
//$$ import net.minecraft.util.ChatComponentText
//#endif
//#if MC > 1.16
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import net.minecraft.util.Formatting
import net.minecraft.text.MutableText
import net.minecraft.text.TextColor
import kotlin.time.Duration.Companion.minutes
//#endif
//#if MC > 1.21
import net.minecraft.text.PlainTextContent
import net.minecraft.client.gui.hud.MessageIndicator
import net.minecraft.network.message.MessageSignatureData
import java.net.URI
import kotlin.jvm.optionals.getOrNull
import kotlin.math.abs
import net.minecraft.text.TranslatableTextContent
//#endif
//#if MC > 1.16
private val unformattedTextCache = TimeLimitedCache<Text, String>(3.minutes)
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

private data class TextCacheKey(val settings: FormattedTextSettings, val component: Text)
//#endif

fun Text.unformattedTextForChatCompat(): String {
//#if MC < 1.16
//$$     return this.unformattedTextForChat
//#elseif MC < 1.21
//$$ return this.asString()
//#else
    return unformattedTextCache.getOrPut(this) {
        computeUnformattedTextCompat()
    }
}

private fun Text.computeUnformattedTextCompat(): String {
    if (this.content is TranslatableTextContent) {
        return this.string
    }
    return (this.content as? PlainTextContent)?.string().orEmpty()
//#endif
}

fun Text.unformattedTextCompat(): String =
//#if MC < 1.16
//$$     this.unformattedText
//#else
iterator().map { it.unformattedTextForChatCompat() }.joinToString(separator = "")
//#endif

// has to be a separate function for pattern mappings
fun Text?.formattedTextCompatLessResets(): String = this.formattedTextCompat(noExtraResets = true)
fun Text?.formattedTextCompatLeadingWhite(): String = this.formattedTextCompat(leadingWhite = true)
fun Text?.formattedTextCompatLeadingWhiteLessResets(): String =
    this.formattedTextCompat(noExtraResets = true, leadingWhite = true)

@JvmOverloads
@Suppress("unused")
fun Text?.formattedTextCompat(noExtraResets: Boolean = false, leadingWhite: Boolean = false): String {
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

private fun Text?.computeFormattedTextCompat(noExtraResets: Boolean, leadingWhite: Boolean): String {
    this ?: return ""
    val sb = StringBuilder()
    for (component in iterator()) {
        val chatStyle = component.style.chatStyle()
        if (leadingWhite || (sb.contains("§") && sb.toString() != "§r") || chatStyle != "§f") {
            sb.append(chatStyle)
        }
        sb.append(component.unformattedTextForChatCompat())
        if (!noExtraResets) {
            sb.append("§r")
        } else {
            if (component == Text.empty()) sb.append("§r")
        }
    }
    return sb.toString().removeSuffix("§r").removePrefix("§r")
}

private val textColorLUT = Formatting.entries
    .mapNotNull { formatting -> formatting.colorValue?.let { it to formatting } }
    .toMap()

fun Style.chatStyle() = buildString {
    color?.let { append(it.toChatFormatting()?.toString() ?: "§r") }
    if (isBold) append("§l")
    if (isItalic) append("§o")
    if (isUnderlined) append("§n")
    if (isStrikethrough) append("§m")
    if (isObfuscated) append("§k")
}

fun TextColor.toChatFormatting(): Formatting? {
    return textColorLUT[this.rgb]
}

fun Text.iterator(): Sequence<Text> {
    return sequenceOf(this) + siblings.asSequence().flatMap { it.iterator() } // TODO: in theory we want to properly inherit styles here
}
//#endif

//#if MC > 1.21
fun MutableText.withColor(formatting: Formatting): Text {
    return this.styled { it.withColor(formatting) }
}
//#endif

fun createResourceLocation(domain: String, path: String): Identifier {
    //#if MC < 1.21
    //$$ val textureLocation = Identifier(domain, path)
    //#else
    val textureLocation = Identifier.of(domain, path)
    //#endif
    return textureLocation
}

fun createResourceLocation(path: String): Identifier {
    //#if MC < 1.21
    //$$ val textureLocation = Identifier(path)
    //#else
    val textureLocation = Identifier.of(path)
    //#endif
    return textureLocation
}

var Text.hover: Text?
    //#if MC < 1.16
    //$$ get() = this.chatStyle.chatHoverEvent?.let { if (it.action == HoverEvent.Action.SHOW_TEXT) it.value else null }
    //#else
    get() = this.style.hoverEvent?.let { if (it.action == HoverEvent.Action.SHOW_TEXT) (it as HoverEvent.ShowText).value else null }
    //#endif
    set(value) {
        //#if MC < 1.16
        //$$ this.chatStyle.chatHoverEvent = value?.let { HoverEvent(HoverEvent.Action.SHOW_TEXT, it) }
        //#else
        value?.let { value -> (this as MutableText).styled { it.withHoverEvent(HoverEvent.ShowText(value)) } }
        //#endif
    }

var Text.command: String?
    //#if MC < 1.21
    //$$ get() = this.style.clickEvent?.let { if (it.action == ClickEvent.Action.RUN_COMMAND) it.value else null }
    //#else
    get() = this.style.clickEvent?.let { if (it.action == ClickEvent.Action.RUN_COMMAND) (it as ClickEvent.RunCommand).command else null }
    //#endif
    set(value) {
        //#if MC < 1.16
        //$$ this.chatStyle.chatClickEvent = value?.let { ClickEvent(ClickEvent.Action.RUN_COMMAND, it) }
        //#else
        (this as MutableText).styled { (it.withClickEvent(ClickEvent.RunCommand(value.orEmpty()))) }
        //#endif
    }

var Text.suggest: String?
    //#if MC < 1.21
    //$$ get() = this.style.clickEvent?.let { if (it.action == ClickEvent.Action.SUGGEST_COMMAND) it.value else null }
    //#else
    get() = this.style.clickEvent?.let { if (it.action == ClickEvent.Action.SUGGEST_COMMAND) (it as ClickEvent.SuggestCommand).command else null }
    //#endif
    set(value) {
        //#if MC < 1.16
        //$$ this.chatStyle.chatClickEvent = value?.let { ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, it) }
        //#else
        (this as MutableText).styled { (it.withClickEvent(ClickEvent.SuggestCommand(value.orEmpty()))) }
        //#endif
    }

var Text.url: String?
    //#if MC < 1.21
    //$$ get() = this.style.clickEvent?.let { if (it.action == ClickEvent.Action.OPEN_URL) it.value else null }
    //#else
    get() = this.style.clickEvent?.let { if (it.action == ClickEvent.Action.OPEN_URL) (it as ClickEvent.OpenUrl).uri.toString() else null }
    //#endif
    set(value) {
        //#if MC < 1.16
        //$$ this.chatStyle.chatClickEvent = value?.let { ClickEvent(ClickEvent.Action.OPEN_URL, it) }
        //#else
        (this as MutableText).styled { (it.withClickEvent(ClickEvent.OpenUrl(URI.create(value.orEmpty())))) }
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
    return this.withHoverEvent(HoverEvent.ShowText(Text.of(text)))
    //#endif
}

fun Style.setHoverShowText(text: Text): Style {
    //#if MC < 1.21
    //$$ return this.setChatHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, text))
    //#else
    return this.withHoverEvent(HoverEvent.ShowText(text))
    //#endif
}

fun Text.appendString(text: String): Text =
    //#if MC < 1.16
    //$$ this.appendText(text)
//#else
(this as MutableText).append(text)
//#endif

fun Text.appendComponent(component: Text): Text =
    //#if MC < 1.16
    //$$ this.appendSibling(component)
//#else
(this as MutableText).append(component)
//#endif

fun addChatMessageToChat(message: Text) {
    //#if FORGE
    //$$ Minecraft.getInstance().player.addChatMessage(message)
    //#else
    MinecraftClient.getInstance().player?.sendMessage(message, false)
    //#endif
}

fun addDeletableMessageToChat(component: Text, id: Int) {
    //#if MC < 1.16
    //$$ Minecraft.getMinecraft().ingameGUI.chatGUI.printChatMessageWithOptionalDeletion(component, id)
    //#else
    MinecraftClient.getInstance().execute {
       MinecraftClient.getInstance().inGameHud.chatHud.removeMessage(idToMessageSignature(id))
       MinecraftClient.getInstance().inGameHud.chatHud.addMessage(component, idToMessageSignature(id), MessageIndicator.system())
    }
    //#endif
}

//#if MC > 1.21
val map = mutableMapOf<Int, MessageSignatureData>()

fun idToMessageSignature(id: Int): MessageSignatureData {
    val newId = abs(id % (255*128))
    if (map.contains(newId)) return map[newId]!!
    val bytes = ByteArray(256)
    val div = newId / 128
    val mod = newId % 128
    for (i in 0 until div) {
        bytes[i] = 127
    }
    bytes[div] = mod.toByte()
    return MessageSignatureData(bytes)
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
    return when (this.action) {
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

fun HoverEvent.value(): Text {
    //#if MC < 1.21
    //$$ return this.contents
    //#else
    return when (this.action) {
        HoverEvent.Action.SHOW_TEXT -> (this as HoverEvent.ShowText).value
        HoverEvent.Action.SHOW_ITEM -> (this as HoverEvent.ShowItem).item.name
        HoverEvent.Action.SHOW_ENTITY -> (this as HoverEvent.ShowEntity).entity.name.getOrNull() ?: Text.empty()
        else -> Text.empty()
    }
    //#endif
}

//#if MC < 1.21
//$$ fun createHoverEvent(action: HoverEvent.Action?, component: TextComponent): HoverEvent? {
//$$     if (action == null) return null
//$$     return HoverEvent(action, component)
//$$ }
//#else
fun createHoverEvent(action: HoverEvent.Action?, component: MutableText): HoverEvent? {
    if (action == null) return null
    when (action) {
        HoverEvent.Action.SHOW_TEXT -> return HoverEvent.ShowText(component)
        // I really don't think anyone is using the other 2 lol
        else -> return null
    }
}
//#endif

fun Text.changeColor(color: LorenzColor): Text =
    //#if MC < 1.21
    //$$ this.shallowCopy().setStyle(this.style.withColor(color.toChatFormatting()))
//#else
this.copy().formatted(color.toChatFormatting())
//#endif

fun Text.convertToJsonString(): String {
    //#if MC < 1.21
    //$$ return Text.Serializer.componentToJson(this)
    //#elseif MC < 1.21.6
    //$$ return Text.Serializer(net.minecraft.registry.DynamicRegistryManager.EMPTY).serialize(this, null, null).toString()
    //#else
    return net.minecraft.text.TextCodecs.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, this).orThrow.toString()
    //#endif
}

//#if MC > 1.21
fun Text.append(newText: Text): Text {
    return (this as MutableText).append(newText)
}

val formattingPattern = Regex("§.(?:§.)?")

fun Text.append(newText: String): Text {
    val mutableText = this as MutableText
    if (mutableText.string.matches(formattingPattern)) {
        return Text.of(mutableText.string + newText)
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
