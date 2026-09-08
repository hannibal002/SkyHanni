package at.hannibal2.skyhanni.utils.chat

import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.center
import at.hannibal2.skyhanni.utils.chat.TextHelper.fitToChat
import at.hannibal2.skyhanni.utils.chat.TextHelper.onClick
import at.hannibal2.skyhanni.utils.chat.TextHelper.send
import at.hannibal2.skyhanni.utils.chat.TextHelper.style
import at.hannibal2.skyhanni.utils.compat.hover
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object PaginatedList {

    fun createDivider(dividerColor: ChatFormatting = ChatFormatting.BLUE) = TextHelper.HYPHEN.fitToChat().style {
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
     * @param formatter A function to format each entry into a Component.
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
    ): Unit = DelayedRun.runOrNextTick("paginated list: $title") {
        val text = mutableListOf<Component>()

        val totalPages = (list.size + maxPerPage - 1) / maxPerPage
        val page = if (totalPages == 0) 0 else currentPage

        text.add(createDivider(dividerColor))
        text.add("§6$title".asComponent().center())

        if (totalPages > 1) {
            text.add(
                TextHelper.join(
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
            text.add(TextHelper.EMPTY)
            text.add("§c$emptyMessage".asComponent().center())
            text.add(TextHelper.EMPTY)
        }

        text.add(createDivider(dividerColor))
        TextHelper.multiline(text).send(chatLineId)
    }
}
