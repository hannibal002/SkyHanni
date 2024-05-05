package at.hannibal2.skyhanni.utils.chat

import net.minecraft.client.Minecraft
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraft.util.IChatComponent

object Text {

    val NEWLINE = "\n".asComponent()
    val HYPHEN = "-".asComponent()

    fun join(vararg components: Any?, separator: IChatComponent? = null): IChatComponent {
        val result = ChatComponentText("")
        components.forEachIndexed { index, it ->
            when (it) {
                is IChatComponent -> result.appendSibling(it)
                is String -> result.appendText(it)
                null -> return@forEachIndexed
                else -> error("Unsupported type: ${it::class.simpleName}")
            }

            if (index < components.size - 1 && separator != null) {
                result.appendSibling(separator)
            }
        }
        return result
    }

    fun String.asComponent(init: IChatComponent.() -> Unit = {}) = ChatComponentText(this).also(init)

    fun IChatComponent.style(init: ChatStyle.() -> Unit): IChatComponent {
        this.chatStyle.init()
        return this
    }

    fun IChatComponent.prefix(prefix: String): IChatComponent = join(prefix, this)
    fun IChatComponent.suffix(suffix: String): IChatComponent = join(this, suffix)
    fun IChatComponent.wrap(prefix: String, suffix: String) = this.prefix(prefix).suffix(suffix)

    fun IChatComponent.width(): Int = Minecraft.getMinecraft().fontRendererObj.getStringWidth(this.formattedText)

    fun IChatComponent.fitToChat(): IChatComponent {
        val width = this.width()
        val maxWidth = Minecraft.getMinecraft().ingameGUI.chatGUI.chatWidth
        if (width < maxWidth) {
            val repeat = maxWidth / width
            val component = ChatComponentText("")
            repeat(repeat) { component.appendSibling(this) }
            return component
        }
        return this
    }
    fun IChatComponent.center(width: Int = Minecraft.getMinecraft().ingameGUI.chatGUI.chatWidth): IChatComponent {
        val textWidth = this.width()
        val spaceWidth = " ".asComponent().width()
        val padding = (width - textWidth) / 2
        return join(" ".repeat(padding / spaceWidth), this)
    }

    fun IChatComponent.send(id: Int = 0) =
        Minecraft.getMinecraft().ingameGUI.chatGUI.printChatMessageWithOptionalDeletion(this, id)

    var IChatComponent.hover: IChatComponent?
        get() = this.chatStyle.chatHoverEvent?.value
        set(value) {
            this.chatStyle.chatHoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, value)
        }

    var IChatComponent.command: String?
        get() = this.chatStyle.chatClickEvent?.let { if (it.action == ClickEvent.Action.RUN_COMMAND) it.value else null }
        set(value) {
            this.chatStyle.chatClickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, value)
        }

    var IChatComponent.suggest: String?
        get() = this.chatStyle.chatClickEvent?.let { if (it.action == ClickEvent.Action.SUGGEST_COMMAND) it.value else null }
        set(value) {
            this.chatStyle.chatClickEvent = ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, value)
        }

    var IChatComponent.url: String?
        get() = this.chatStyle.chatClickEvent?.let { if (it.action == ClickEvent.Action.OPEN_URL) it.value else null }
        set(value) {
            this.chatStyle.chatClickEvent = ClickEvent(ClickEvent.Action.OPEN_URL, value)
        }
}
