package at.hannibal2.skyhanni.utils.chat

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.addDeletableMessageToChat
import at.hannibal2.skyhanni.utils.compat.appendString
import at.hannibal2.skyhanni.utils.compat.command
import at.hannibal2.skyhanni.utils.compat.hover
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatStyle
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.IChatComponent
//#if MC < 1.21
import net.minecraft.util.ChatComponentText
//#endif
//#if MC > 1.16
//$$ import net.minecraft.network.chat.MutableComponent
//#endif

object TextHelper {

    val NEWLINE = "\n".asComponent()
    val HYPHEN = "-".asComponent()
    val SPACE = " ".asComponent()
    val EMPTY = "".asComponent()

    //#if MC < 1.16
    fun text(text: String, init: IChatComponent.() -> Unit = {}) = text.asComponent(init)
    fun String.asComponent(init: IChatComponent.() -> Unit = {}) = ChatComponentText(this).also(init)
    //#elseif MC < 1.21
    //$$ fun text(text: String, init: MutableComponent.() -> Unit = {}) = text.asComponent(init)
    //$$ fun String.asComponent(init: MutableComponent.() -> Unit = {}) = (TextComponent(this) as MutableComponent).also(init)
    //#else
    //$$ fun text(text: String, init: MutableText.() -> Unit = {}) = text.asComponent(init)
    //$$ fun String.asComponent(init: MutableText.() -> Unit = {}): MutableText = (Text.of(this) as MutableText).also(init)
    //#endif

    fun multiline(vararg lines: Any?) = join(*lines, separator = NEWLINE)
    fun join(vararg components: Any?, separator: IChatComponent? = null): IChatComponent {
        val result = "".asComponent()
        components.forEachIndexed { index, component ->
            when (component) {
                is IChatComponent -> result.appendSibling(component)
                is String -> result.appendString(component)
                is List<*> -> result.appendSibling(join(*component.toTypedArray(), separator = separator))
                null -> return@forEachIndexed
                else -> error("Unsupported type: ${component::class.simpleName}")
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
            val component = "".asComponent()
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
        addDeletableMessageToChat(this, id)

    fun List<IChatComponent>.send(id: Int = 0) {
        val parent = "".asComponent()
        forEach {
            parent.siblings.add(it)
            parent.siblings.add("\n".asComponent())
        }

        parent.send(id)
    }

    fun IChatComponent.onClick(expiresAt: SimpleTimeMark = SimpleTimeMark.farFuture(), oneTime: Boolean = true, onClick: () -> Any) {
        val token = ChatClickActionManager.createAction(onClick, expiresAt, oneTime)
        this.command = "/shaction $token"
    }

    fun IChatComponent.onHover(tip: String) {
        this.hover = tip.asComponent()
    }

    fun IChatComponent.onHover(tips: List<String>) {
        this.hover = tips.joinToString("\n").asComponent()
    }

    fun createDivider(dividerColor: EnumChatFormatting = EnumChatFormatting.BLUE) = HYPHEN.fitToChat().style {
        setStrikethrough(true)
        color = dividerColor
    }

    /**
     * Displays a paginated list of entries in the chat.
     *
     * @param title The title of the paginated list.
     * @param list The list of entries to paginate and display.
     * @param chatLineId The ID of the chat line for message updates.
     * @param emptyMessage The message to display if the list is empty.
     * @param currentPage The current page to display.
     * @param maxPerPage The number of entries to display per page.
     * @param dividerColor The color of the divider lines.
     * @param formatter A function to format each entry into an IChatComponent.
     */
    fun <T> displayPaginatedList(
        title: String,
        list: List<T>,
        chatLineId: Int,
        emptyMessage: String,
        currentPage: Int = 1,
        maxPerPage: Int = 15,
        dividerColor: EnumChatFormatting = EnumChatFormatting.BLUE,
        formatter: (T) -> IChatComponent,
    ) {
        val text = mutableListOf<IChatComponent>()

        val totalPages = (list.size + maxPerPage - 1) / maxPerPage
        val page = if (totalPages == 0) 0 else currentPage

        text.add(createDivider(dividerColor))
        text.add("§6$title".asComponent().center())

        if (totalPages > 1) {
            text.add(
                join(
                    if (page > 1) "§6§l<<".asComponent {
                        hover = "§eClick to view page ${page - 1}".asComponent()
                        onClick {
                            displayPaginatedList(title, list, chatLineId, emptyMessage, page - 1, maxPerPage, dividerColor, formatter)
                        }
                    } else null,
                    " ",
                    "§6(Page $page of $totalPages)",
                    " ",
                    if (page < totalPages) "§6§l>>".asComponent {
                        hover = "§eClick to view page ${page + 1}".asComponent()
                        onClick {
                            displayPaginatedList(title, list, chatLineId, emptyMessage, page + 1, maxPerPage, dividerColor, formatter)
                        }
                    } else null,
                ).center(),
            )
        }

        text.add(createDivider(dividerColor))

        if (list.isNotEmpty()) {
            val start = (page - 1) * maxPerPage
            val end = (page * maxPerPage).coerceAtMost(list.size)
            for (i in start until end) {
                text.add(formatter(list[i]))
            }
        } else {
            text.add(EMPTY)
            text.add("§c$emptyMessage".asComponent().center())
            text.add(EMPTY)
        }

        text.add(createDivider(dividerColor))
        multiline(text).send(chatLineId)
    }
}
