package at.hannibal2.skyhanni.utils.renderables.container.table

import at.hannibal2.skyhanni.utils.renderables.Renderable

/**
 * @property content Collection of rows of Renderables
 * @property xSpacing Space between rows
 * @property ySpacing Space between columns
 */
interface TabularRenderable<C : Collection<Renderable>, R : Collection<C>> : Renderable {
    val content: R
    val xSpacing: Int
    val ySpacing: Int
}

interface TabularRenderableWithRowRender<C : Collection<Renderable>, R : Collection<C>> : TabularRenderable<C, R> {

    fun renderRow(mouseOffsetX: Int, mouseOffsetY: Int, rowIndex: Int, row: C)

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        content.forEachIndexed { index, row -> renderRow(mouseOffsetX, mouseOffsetY, index, row) }
    }
}

interface TabularRenderableWithCellRender<C : Collection<Renderable>, R : Collection<C>> : TabularRenderableWithRowRender<C, R> {

    fun renderCell(mouseOffsetX: Int, mouseOffsetY: Int, rowIndex: Int, columnIndex: Int, renderable: Renderable)

    override fun renderRow(mouseOffsetX: Int, mouseOffsetY: Int, rowIndex: Int, row: C) {
        row.forEachIndexed { index, cell -> renderCell(mouseOffsetX, mouseOffsetY, rowIndex, index, cell) }
    }
}

