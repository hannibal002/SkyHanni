package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment

data class TableRow(
    val cells: List<Renderable>,
    val searchKey: String = ""
)

open class RenderableTable(
    initialRows: List<TableRow> = emptyList(),
    private val xPadding: Int = 1,
    private val yPadding: Int = 0,
    private val useEmptySpace: Boolean = false,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
) : Renderable {
    protected val rows = initialRows.toMutableList()
    open fun addRow(row: TableRow) = rows.add(row)

    private val xOffsets: List<Int>
        get() = RenderableUtils.calculateTableXOffsets(rows.map { it.cells }, xPadding)
    private val yOffsets: List<Int>
        get() = RenderableUtils.calculateTableYOffsets(rows.map { it.cells }, yPadding)

    override val width: Int
        get() = xOffsets.last() - xPadding
    override val height: Int
        get() = yOffsets.last() - yPadding

    open val renderable: Renderable
        get() = Renderable.table(
            rows.map { it.cells },
            xPadding,
            yPadding,
            useEmptySpace,
            horizontalAlign,
            verticalAlign
        )

    override fun render(posX: Int, posY: Int) = renderable.render(posX, posY)
}

open class SearchableRenderableTable(
    initialRows: List<TableRow> = emptyList(),
    xPadding: Int = 1,
    yPadding: Int = 0,
    useEmptySpace: Boolean = false,
    horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
    private val header: List<Renderable> = emptyList(),
    private val searchInputGetter: () -> SearchTextInput,
) : RenderableTable(initialRows, xPadding, yPadding, useEmptySpace, horizontalAlign, verticalAlign) {

    override val renderable: Renderable
        get() = rows.associate { it.cells to it.searchKey }
            .buildSearchableTable(searchInputGetter, header)

    fun addRow(cells: Collection<Renderable>, search: String = "") {
        rows.add(TableRow(cells.toList(), search))
    }
}

class SearchableScrollableRenderableTable(
    initialRows: List<TableRow> = emptyList(),
    xPadding: Int = 1,
    yPadding: Int = 0,
    useEmptySpace: Boolean = false,
    horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
    private val header: List<Renderable> = emptyList(),
    private val searchInputGetter: () -> SearchTextInput,
    private val scrollValueGetter: () -> ScrollValue = { ScrollValue() },
    private val maxHeightGetter: () -> Int = { 200 },
) : SearchableRenderableTable(initialRows, xPadding, yPadding, useEmptySpace, horizontalAlign, verticalAlign, header, searchInputGetter) {

    override val renderable: Renderable
        get() = rows.associate { it.cells to it.searchKey }
            .buildSearchableTable(
                searchInputGetter,
                header,
                scrollValueGetter,
                maxHeight = maxHeightGetter.invoke(),
            )
}

