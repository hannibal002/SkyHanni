package at.hannibal2.skyhanni.utils.chat

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.Minecraft
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraft.util.IChatComponent

object Text {

    val NEWLINE = "\n".asComponent()
    val HYPHEN = "-".asComponent()
    val SPACE = " ".asComponent()
    val EMPTY = "".asComponent()

    fun text(text: String, init: IChatComponent.() -> Unit = {}) = text.asComponent(init)
    fun String.asComponent(init: IChatComponent.() -> Unit = {}) = ChatComponentText(this).also(init)

    fun multiline(vararg lines: Any?) = join(*lines, separator = NEWLINE)
    fun join(vararg components: Any?, separator: IChatComponent? = null): IChatComponent {
        val result = ChatComponentText("")
        components.forEachIndexed { index, it ->
            when (it) {
                is IChatComponent -> result.appendSibling(it)
                is String -> result.appendText(it)
                is List<*> -> result.appendSibling(join(*it.toTypedArray(), separator = separator))
                null -> return@forEachIndexed
                else -> error("Unsupported type: ${it::class.simpleName}")
            }

            if (index < components.size - 1 && separator != null) {
                result.appendSibling(separator)
            }
        }
        return result
    }

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
        val spaceWidth = SPACE.width()
        val padding = (width - textWidth) / 2
        return join(" ".repeat(padding / spaceWidth), this)
    }

    fun IChatComponent.send(id: Int = 0) =
        Minecraft.getMinecraft().ingameGUI.chatGUI.printChatMessageWithOptionalDeletion(this, id)

    var IChatComponent.hover: IChatComponent?
        get() = this.chatStyle.chatHoverEvent?.value
        set(value) {
            this.chatStyle.chatHoverEvent = value?.let { HoverEvent(HoverEvent.Action.SHOW_TEXT, it) }
        }

    var IChatComponent.command: String?
        get() = this.chatStyle.chatClickEvent?.let { if (it.action == ClickEvent.Action.RUN_COMMAND) it.value else null }
        set(value) {
            this.chatStyle.chatClickEvent = value?.let { ClickEvent(ClickEvent.Action.RUN_COMMAND, it) }
        }

    var IChatComponent.suggest: String?
        get() = this.chatStyle.chatClickEvent?.let { if (it.action == ClickEvent.Action.SUGGEST_COMMAND) it.value else null }
        set(value) {
            this.chatStyle.chatClickEvent = value?.let { ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, it) }
        }

    var IChatComponent.url: String?
        get() = this.chatStyle.chatClickEvent?.let { if (it.action == ClickEvent.Action.OPEN_URL) it.value else null }
        set(value) {
            this.chatStyle.chatClickEvent = value?.let { ClickEvent(ClickEvent.Action.OPEN_URL, it) }
        }

    fun IChatComponent.onClick(expiresAt: SimpleTimeMark = SimpleTimeMark.farFuture(), oneTime: Boolean = true, onClick: () -> Any) {
        val token = ChatClickActionManager.createAction(onClick, expiresAt, oneTime)
        this.command = "/shaction $token"
    }
    
}
