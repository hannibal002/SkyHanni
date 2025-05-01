package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.Minecraft
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatStyle
import net.minecraft.util.IChatComponent
import net.minecraft.util.ResourceLocation
//#if MC < 1.16
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
//#endif
//#if MC > 1.16
//$$ import net.minecraft.ChatFormatting
//$$ import net.minecraft.network.chat.MutableComponent
//$$ import net.minecraft.network.chat.TextColor
//#endif
//#if MC > 1.21
//$$ import net.minecraft.text.PlainTextContent
//$$ import net.minecraft.client.gui.hud.MessageIndicator
//$$ import net.minecraft.network.message.MessageSignatureData
//$$ import java.net.URI
//$$ import kotlin.jvm.optionals.getOrNull
//$$ import kotlin.math.abs
//#endif

fun IChatComponent.unformattedTextForChatCompat(): String =
//#if MC < 1.16
    this.unformattedTextForChat
//#elseif MC < 1.21
//$$ this.contents
//#else
//$$ (this.content as? PlainTextContent)?.string().orEmpty()
//#endif

fun IChatComponent.unformattedTextCompat(): String =
//#if MC < 1.16
    this.unformattedText
//#else
//$$ iterator().map { it.unformattedTextForChatCompat() }.joinToString(separator = "")
//#endif

fun IChatComponent?.formattedTextCompat(): String =
//#if MC < 1.16
    this?.formattedText.orEmpty()
//#else
//$$ run {
//$$     this ?: return@run ""
//$$     val sb = StringBuilder()
//$$     for (component in iterator()) {
//$$         sb.append(component.style.color?.toChatFormatting()?.toString() ?: "§r")
//$$         sb.append(component.unformattedTextForChatCompat())
//$$         sb.append("§r")
//$$     }
//$$     sb.toString()
//$$ }
//$$
//$$ private val textColorLUT = ChatFormatting.entries
//$$     .mapNotNull { formatting -> formatting.color?.let { it to formatting } }
//$$     .toMap()
//$$
//$$ fun TextColor.toChatFormatting(): ChatFormatting? {
//$$     return textColorLUT[this.value]
//$$ }
//$$
//$$ fun Component.iterator(): Sequence<Component> {
//$$     return sequenceOf(this) + siblings.asSequence().flatMap { it.iterator() } // TODO: in theory we want to properly inherit styles here
//$$ }
//#endif

//#if MC > 1.21
//$$ fun MutableText.withColor(formatting: Formatting): Text {
//$$     return this.styled { it.withColor(formatting) }
//$$ }
//#endif

fun createResourceLocation(domain: String, path: String): ResourceLocation {
    //#if MC < 1.21
    val textureLocation = ResourceLocation(domain, path)
    //#else
    //$$ val textureLocation = Identifier.of(domain, path)
    //#endif
    return textureLocation
}

fun createResourceLocation(path: String): ResourceLocation {
    //#if MC < 1.21
    val textureLocation = ResourceLocation(path)
    //#else
    //$$ val textureLocation = Identifier.of(path)
    //#endif
    return textureLocation
}

var IChatComponent.hover: IChatComponent?
    //#if MC < 1.16
    get() = this.chatStyle.chatHoverEvent?.let { if (it.action == HoverEvent.Action.SHOW_TEXT) it.value else null }
    //#else
    //$$ get() = this.style.hoverEvent?.let { if (it.action == HoverEvent.Action.SHOW_TEXT) (it as HoverEvent.ShowText).value else null }
    //#endif
    set(value) {
        //#if MC < 1.16
        this.chatStyle.chatHoverEvent = value?.let { HoverEvent(HoverEvent.Action.SHOW_TEXT, it) }
        //#else
        //$$ this.style.withHoverEvent(value?.let {  HoverEvent.ShowText(it) })
        //#endif
    }

var IChatComponent.command: String?
    //#if MC < 1.21
    get() = this.chatStyle.chatClickEvent?.let { if (it.action == ClickEvent.Action.RUN_COMMAND) it.value else null }
    //#else
    //$$ get() = this.style.clickEvent?.let { if (it.action == ClickEvent.Action.RUN_COMMAND) (it as ClickEvent.RunCommand).command else null }
    //#endif
    set(value) {
        //#if MC < 1.16
        this.chatStyle.chatClickEvent = value?.let { ClickEvent(ClickEvent.Action.RUN_COMMAND, it) }
        //#else
        //$$ this.style.withClickEvent(value?.let { ClickEvent.RunCommand(it) })
        //#endif
    }

var IChatComponent.suggest: String?
    //#if MC < 1.21
    get() = this.chatStyle.chatClickEvent?.let { if (it.action == ClickEvent.Action.SUGGEST_COMMAND) it.value else null }
    //#else
    //$$ get() = this.style.clickEvent?.let { if (it.action == ClickEvent.Action.SUGGEST_COMMAND) (it as ClickEvent.SuggestCommand).command else null }
    //#endif
    set(value) {
        //#if MC < 1.16
        this.chatStyle.chatClickEvent = value?.let { ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, it) }
        //#else
        //$$ this.style.withClickEvent(value?.let { ClickEvent.SuggestCommand(it) })
        //#endif
    }

var IChatComponent.url: String?
    //#if MC < 1.21
    get() = this.chatStyle.chatClickEvent?.let { if (it.action == ClickEvent.Action.OPEN_URL) it.value else null }
    //#else
    //$$ get() = this.style.clickEvent?.let { if (it.action == ClickEvent.Action.OPEN_URL) (it as ClickEvent.OpenUrl).uri.toString() else null }
    //#endif
    set(value) {
        //#if MC < 1.16
        this.chatStyle.chatClickEvent = value?.let { ClickEvent(ClickEvent.Action.OPEN_URL, it) }
        //#else
        //$$ this.style.withClickEvent(value?.let { ClickEvent.OpenUrl(URI.create(it)) })
        //#endif
    }

fun ChatStyle.setClickRunCommand(text: String): ChatStyle {
    //#if MC < 1.21
    return this.setChatClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, text))
    //#else
    //$$ return this.withClickEvent(ClickEvent.RunCommand(text))
    //#endif
}

fun ChatStyle.setHoverShowText(text: String): ChatStyle {
    //#if MC < 1.21
    return this.setChatHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, text.asComponent()))
    //#else
    //$$ return this.withHoverEvent(HoverEvent.ShowText(Text.of(text)))
    //#endif
}

fun ChatStyle.setHoverShowText(text: IChatComponent): ChatStyle {
    //#if MC < 1.21
    return this.setChatHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, text))
    //#else
    //$$ return this.withHoverEvent(HoverEvent.ShowText(text))
    //#endif
}

fun IChatComponent.appendString(text: String): IChatComponent =
    //#if MC < 1.16
    this.appendText(text)
//#else
//$$ (this as MutableComponent).append(text)
//#endif

fun IChatComponent.appendComponent(component: IChatComponent): IChatComponent =
    //#if MC < 1.16
    this.appendSibling(component)
//#else
//$$ (this as MutableComponent).append(component)
//#endif

fun addChatMessageToChat(message: IChatComponent) {
    //#if FORGE
    Minecraft.getMinecraft().thePlayer.addChatMessage(message)
    //#else
    //$$ MinecraftClient.getInstance().player?.sendMessage(message, false)
    //#endif
}

fun addDeletableMessageToChat(component: IChatComponent, id: Int) {
    //#if MC < 1.16
    Minecraft.getMinecraft().ingameGUI.chatGUI.printChatMessageWithOptionalDeletion(component, id)
    //#else
    //$$ MinecraftClient.getInstance().inGameHud.chatHud.addMessage(component, idToMessageSignature(id), MessageIndicator.system())
    //#endif
}

//#if MC > 1.21
//$$ val map = mutableMapOf<Int, MessageSignatureData>()
//$$
//$$ fun idToMessageSignature(id: Int): MessageSignatureData {
//$$     val newId = abs(id % (255*128))
//$$     if (map.contains(newId)) return map[newId]!!
//$$     val bytes = ByteArray(256)
//$$     val div = newId / 128
//$$     val mod = newId % 128
//$$     for (i in 0 until div) {
//$$         bytes[i] = 127
//$$     }
//$$     bytes[div] = mod.toByte()
//$$     return MessageSignatureData(bytes)
//$$ }
//#endif

val defaultStyleConstructor: ChatStyle get() =
    //#if MC < 1.16
    ChatStyle()
//#else
//$$ Style.EMPTY
//#endif

fun ClickEvent.value(): String {
    //#if MC < 1.21
    return this.value
    //#else
    //$$ return when (this.action) {
    //$$     ClickEvent.Action.OPEN_URL -> (this as ClickEvent.OpenUrl).uri.toString()
    //$$     ClickEvent.Action.RUN_COMMAND -> (this as ClickEvent.RunCommand).command
    //$$     ClickEvent.Action.SUGGEST_COMMAND -> (this as ClickEvent.SuggestCommand).command
    //$$     // we don't use these bottom 3 but might as well have them here
    //$$     ClickEvent.Action.CHANGE_PAGE -> (this as ClickEvent.ChangePage).page.toString()
    //$$     ClickEvent.Action.COPY_TO_CLIPBOARD -> (this as ClickEvent.CopyToClipboard).value
    //$$     ClickEvent.Action.OPEN_FILE -> (this as ClickEvent.OpenFile).path
    //$$     // todo use error manager here probably, not doing it now because it doesnt compile on 1.21
    //$$     else -> ""
    //$$ }
    //#endif

}

fun HoverEvent.value(): IChatComponent {
    //#if MC < 1.21
    return this.value
    //#else
    //$$ return when (this.action) {
    //$$     HoverEvent.Action.SHOW_TEXT -> (this as HoverEvent.ShowText).value
    //$$     HoverEvent.Action.SHOW_ITEM -> (this as HoverEvent.ShowItem).item.name
    //$$     HoverEvent.Action.SHOW_ENTITY -> (this as HoverEvent.ShowEntity).entity.name.getOrNull() ?: Text.empty()
    //$$     else -> Text.empty()
    //$$ }
    //#endif
}
