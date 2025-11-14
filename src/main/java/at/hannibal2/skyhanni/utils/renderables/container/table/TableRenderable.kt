package at.hannibal2.skyhanni.utils.renderables.container.table

import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned

class TableRenderable private constructor(
    override val content: List<List<Renderable>>,
    override val xSpacing: Int = 1,
    override val ySpacing: Int = 0,
    useEmptySpace: Boolean = false,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
) : TabularRenderableWithCellRender<List<Renderable>, List<List<Renderable>>> {

    private val xOffsets: List<Int> = RenderableUtils.calculateTableXOffsets(content, xSpacing)
    private val yOffsets: List<Int> = RenderableUtils.calculateTableYOffsets(content, ySpacing)

    private val emptySpaceX = if (useEmptySpace) 0 else xSpacing
    private val emptySpaceY = if (useEmptySpace) 0 else ySpacing

    override val width = xOffsets.last() - emptySpaceX
    override val height = yOffsets.last() - emptySpaceY

    override fun renderCell(mouseOffsetX: Int, mouseOffsetY: Int, rowIndex: Int, columnIndex: Int, renderable: Renderable) {
        DrawContextUtils.pushPop {
            DrawContextUtils.translate(xOffsets[columnIndex].toFloat(), yOffsets[rowIndex].toFloat(), 0F)
            renderable.renderXYAligned(
                mouseOffsetX + xOffsets[columnIndex],
                mouseOffsetY + yOffsets[rowIndex],
                xOffsets[columnIndex + 1] - xOffsets[columnIndex] - emptySpaceX,
                yOffsets[rowIndex + 1] - yOffsets[rowIndex] - emptySpaceY,
            )
        }
    }

    companion object {
        /**
         * @property content Collection of rows of Renderables
         * @property xSpacing Space between rows
         * @property ySpacing Space between columns
         */
        fun Renderable.Companion.table(
            content: List<List<Renderable>>,
            xSpacing: Int = 1,
            ySpacing: Int = 0,
            useEmptySpace: Boolean = false,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = TableRenderable(content, xSpacing, ySpacing, useEmptySpace, horizontalAlign, verticalAlign)
    }
}
