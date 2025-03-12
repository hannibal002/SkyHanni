package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment

open class RenderableTable(
    content: List<List<Renderable>> = mutableListOf(),
    private val xPadding: Int = 1,
    private val yPadding: Int = 0,
    private val useEmptySpace: Boolean = false,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
) : Renderable {
    protected val backingList: MutableList<List<Renderable>> = content.toMutableList()
    fun addRow(specifiedRow: Collection<Renderable>) = backingList.add(specifiedRow.toList())

    private val xOffsets: List<Int> = RenderableUtils.calculateTableXOffsets(content, xPadding)
    private val yOffsets: List<Int> = RenderableUtils.calculateTableYOffsets(content, yPadding)

    override val width = xOffsets.last() - xPadding
    override val height = yOffsets.last() - yPadding

    open val renderable get() = Renderable.table(
        backingList,
        xPadding,
        yPadding,
        useEmptySpace,
        horizontalAlign,
        verticalAlign
    )

    override fun render(posX: Int, posY: Int) = renderable.render(posX, posY)
}

class SearchableRenderableTable(
    content: Map<List<Renderable>, String> = mutableMapOf(),
    xPadding: Int = 1,
    yPadding: Int = 0,
    useEmptySpace: Boolean = false,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
    private val searchInputGetter: () -> SearchTextInput,
) : RenderableTable(
    content.keys.toList(), xPadding, yPadding, useEmptySpace, horizontalAlign, verticalAlign
) {
    private val backingMap = content.toMutableMap()
    override val renderable get() = backingMap.buildSearchableTable(searchInputGetter.invoke())

    fun addRow(specifiedRow: List<Renderable>, search: String) {
        backingList.add(specifiedRow)
        backingMap[specifiedRow] = search
    }
}
