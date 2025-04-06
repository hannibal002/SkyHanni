package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.Minecraft
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatStyle
import net.minecraft.util.IChatComponent
import net.minecraft.util.ResourceLocation
//#if MC > 1.16
//$$ import net.minecraft.ChatFormatting
//$$ import net.minecraft.network.chat.MutableComponent
//$$ import net.minecraft.network.chat.TextColor
//#endif
//#if MC > 1.21
//$$ import net.minecraft.text.PlainTextContent
//$$ import net.minecraft.client.gui.hud.MessageIndicator
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
    //$$ get() = this.style.hoverEvent?.let { if (it.action == HoverEvent.Action.SHOW_TEXT) it.getValue(HoverEvent.Action.SHOW_TEXT) else null }
    //#endif
    set(value) {
        //#if MC < 1.16
        this.chatStyle.chatHoverEvent = value?.let { HoverEvent(HoverEvent.Action.SHOW_TEXT, it) }
        //#else
        //$$ this.style.withHoverEvent(value?.let { HoverEvent(HoverEvent.Action.SHOW_TEXT, it) })
        //#endif
    }

var IChatComponent.command: String?
    get() = this.chatStyle.chatClickEvent?.let { if (it.action == ClickEvent.Action.RUN_COMMAND) it.value else null }
    set(value) {
        //#if MC < 1.16
        this.chatStyle.chatClickEvent = value?.let { ClickEvent(ClickEvent.Action.RUN_COMMAND, it) }
        //#else
        //$$ this.style.withClickEvent(value?.let { ClickEvent(ClickEvent.Action.RUN_COMMAND, it) })
        //#endif
    }

var IChatComponent.suggest: String?
    get() = this.chatStyle.chatClickEvent?.let { if (it.action == ClickEvent.Action.SUGGEST_COMMAND) it.value else null }
    set(value) {
        //#if MC < 1.16
        this.chatStyle.chatClickEvent = value?.let { ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, it) }
        //#else
        //$$ this.style.withClickEvent(value?.let { ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, it) })
        //#endif
    }

var IChatComponent.url: String?
    get() = this.chatStyle.chatClickEvent?.let { if (it.action == ClickEvent.Action.OPEN_URL) it.value else null }
    set(value) {
        //#if MC < 1.16
        this.chatStyle.chatClickEvent = value?.let { ClickEvent(ClickEvent.Action.OPEN_URL, it) }
        //#else
        //$$ this.style.withClickEvent(value?.let { ClickEvent(ClickEvent.Action.OPEN_URL, it) })
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
    //$$ // todo convert the id int to the middle variable of MessageSignatureData
    //$$ MinecraftClient.getInstance().inGameHud.chatHud.addMessage(component, null, MessageIndicator.system())
    //#endif
}

val defaultStyleConstructor: ChatStyle get() =
    //#if MC < 1.16
    ChatStyle()
//#else
//$$ Style.EMPTY
//#endif
