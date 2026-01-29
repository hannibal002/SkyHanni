package at.hannibal2.skyhanni.utils.chat

import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.addDeletableMessageToChat
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.command
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.hover
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import java.awt.Color

@Suppress("TooManyFunctions")
object TextHelper {

    val NEWLINE = "\n".asComponent()
    val HYPHEN = "-".asComponent()
    val SPACE = " ".asComponent()
    val EMPTY = "".asComponent()

    fun text(text: String, init: MutableComponent.() -> Unit = {}) = text.asComponent(init)
    fun String.asComponent(init: MutableComponent.() -> Unit = {}): MutableComponent =
        Component.literal(this).also(init)

    fun multiline(vararg lines: Any?) = join(*lines, separator = NEWLINE)
    fun join(vararg components: Any?, separator: Component? = null): Component {
        val result = "".asComponent()
        components.forEachIndexed { index, component ->
            when (component) {
                is Component -> result.append(component)
                is String -> result.append(component)
                is List<*> -> result.append(join(*component.toTypedArray(), separator = separator))
                null -> return@forEachIndexed
                else -> error("Unsupported type: ${component::class.simpleName}")
            }

            if (index < components.size - 1 && separator != null) {
                result.append(separator)
            }
        }
        return result
    }

    fun Component.style(init: Style.() -> Unit): Component {
        this.style.init()
        return this
    }

    fun Component.prefix(prefix: String): Component = join(prefix, this)
    fun Component.suffix(suffix: String): Component = join(this, suffix)
    fun Component.wrap(prefix: String, suffix: String) = this.prefix(prefix).suffix(suffix)

    fun Component.width(): Int = Minecraft.getInstance().font.width(this.string)

    fun Component.fitToChat(): Component {
        val width = this.width()
        val maxWidth = Minecraft.getInstance().gui.chat.width
        if (width < maxWidth) {
            val repeat = maxWidth / width
            val component = "".asComponent()
            repeat(repeat) { component.append(this) }
            return component
        }
        return this
    }

    fun Component.center(width: Int = Minecraft.getInstance().gui.chat.width): Component {
        val textWidth = this.width()
        val spaceWidth = SPACE.width()
        val padding = (width - textWidth) / 2
        return join(" ".repeat(padding / spaceWidth), this)
    }

    fun Component.send(id: Int = 0, bypassSelfMessages: Boolean = false) =
        addDeletableMessageToChat(this, id, bypassSelfMessages)

    fun List<Component>.send(id: Int = 0, bypassSelfMessages: Boolean = false) {
        val parent = "".asComponent()
        forEach {
            parent.siblings.add(it)
            parent.siblings.add("\n".asComponent())
        }

        parent.send(id, bypassSelfMessages)
    }

    fun Component.onClick(expiresAt: SimpleTimeMark = SimpleTimeMark.farFuture(), oneTime: Boolean = true, onClick: () -> Any) {
        val token = ChatClickActionManager.createAction(onClick, expiresAt, oneTime)
        this.command = "/shaction $token"
    }

    fun Component.onHover(tip: String) {
        this.hover = tip.asComponent()
    }

    fun Component.onHover(tips: List<String>) {
        this.hover = tips.joinToString("\n").asComponent()
    }

    fun createDivider(dividerColor: ChatFormatting = ChatFormatting.BLUE) = HYPHEN.fitToChat().style {
        withStrikethrough(true)
        withColor(dividerColor)
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
        dividerColor: ChatFormatting = ChatFormatting.BLUE,
        formatter: (T) -> Component,
    ) {
        val text = mutableListOf<Component>()

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

    fun createGradientText(start: LorenzColor, end: LorenzColor, string: String): Component {
        return createGradientText(start.toColor(), end.toColor(), string)
    }

    fun createGradientText(start: Color, end: Color, string: String): Component {
        val length = string.length
        val text = componentBuilder {
            for ((index, char) in string.withIndex()) {
                val color = ColorUtils.blendRGB(start, end, index, length).rgb
                append(char.toString()) {
                    withColor(color)
                }
            }
        }
        return text
    }

    private val chromaStyle by lazy { TextColor(0xFFFFFE, "chroma") }

    fun getChromaColorStyle(): TextColor {
        return chromaStyle
    }
}
