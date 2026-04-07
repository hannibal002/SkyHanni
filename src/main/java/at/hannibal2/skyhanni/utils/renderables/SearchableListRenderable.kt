package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.compat.SkyHanniScreenTheme
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.textInput

/**
 * A vertically stacked search field above a filtered scroll list.
 *
 * @param items Pairs of (key used for filtering, Renderable to display).
 * @param height The pixel height of the scroll list area.
 * @param scrollValue The [ScrollValue] controlling scroll position.
 * @param searchInput The [TextInput] holding the current search query.
 * @param isSearchActive Supplier returning true when the search field has focus.
 * @param onSearchActivate Called when the user clicks the search field.
 * @param filterPredicate Returns true when an item should appear for the given query.
 */
fun Renderable.Companion.searchableList(
    items: List<Pair<String, Renderable>>,
    height: Int,
    scrollValue: ScrollValue,
    searchInput: TextInput,
    isSearchActive: () -> Boolean,
    onSearchActivate: () -> Unit,
    filterPredicate: (query: String, key: String) -> Boolean = { query, key -> query.lowercase() in key.lowercase() },
): Renderable {
    val maxWidth = items.maxOfOrNull { it.second.width } ?: 200
    val searchField = Renderable.textInput(
        textInput = searchInput,
        isActive = isSearchActive,
        onActivate = onSearchActivate,
        width = maxWidth,
    )
    val filteredList = object : Renderable {
        override val width = maxWidth
        override val height = height
        override val horizontalAlign = HorizontalAlignment.LEFT
        override val verticalAlign = VerticalAlignment.TOP

        override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
            val query = searchInput.textBox
            val filtered = if (query.isEmpty()) items.map { it.second }
            else items.filter { filterPredicate(query, it.first) }.map { it.second }
            Renderable.scrollList(
                filtered,
                height,
                scrollValue = scrollValue,
                showScrollbar = true,
                scrollbarTrackColor = SkyHanniScreenTheme.COLOR_SCROLLBAR_TRACK,
                scrollbarThumbColor = SkyHanniScreenTheme.COLOR_SCROLLBAR_THUMB,
            ).render(mouseOffsetX, mouseOffsetY)
        }
    }
    return Renderable.vertical(listOf(searchField, filteredList), spacing = 4)
}
