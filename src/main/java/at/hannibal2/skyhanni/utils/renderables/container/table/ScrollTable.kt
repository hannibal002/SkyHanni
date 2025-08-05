package at.hannibal2.skyhanni.utils.renderables.container.table

import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.shouldAllowLink
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.ScrollInput
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import at.hannibal2.skyhanni.utils.renderables.container.SlidingWindow
import at.hannibal2.skyhanni.utils.renderables.container.relativeProvider

class ScrollTable private constructor(
    override val content: List<List<Renderable>>,
    override val height: Int,
    scrollValue: ScrollValue = ScrollValue(),
    velocity: Double = 2.0,
    button: Int? = null,
    override val xSpacing: Int = 1,
    override val ySpacing: Int = 0,
    private val header: List<Renderable> = emptyList(),
    private val bypassChecks: Boolean = false,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
) : TabularRenderableWithCellRender<List<Renderable>, List<List<Renderable>>>, SlidingWindow {

    private val xOffsets: List<Int>
    private val yOffsets: List<Int>
    private val hasHeader = header.isNotEmpty()

    init {
        val contentPlusHeader = if (hasHeader) listOf(header) + content else content
        xOffsets = RenderableUtils.calculateTableXOffsets(contentPlusHeader, xSpacing)
        yOffsets = RenderableUtils.calculateTableYOffsets(contentPlusHeader, ySpacing)
    }

    override val width = xOffsets.last() - xSpacing

    override val windowMin = if (hasHeader) yOffsets[1] else 0
    override val windowSize = height - windowMin
    override val windowMax = yOffsets.last() - ySpacing

    private val getRange = relativeProvider<Int> { it }

    override val scroll = ScrollInput.Companion.Vertical(
        scrollValue = scrollValue,
        minHeight = lowerBound,
        maxHeight = upperBound,
        velocity = velocity,
        dragScrollMouseButton = button,
    )

    private var renderY = 0

    override fun renderCell(mouseOffsetX: Int, mouseOffsetY: Int, rowIndex: Int, columnIndex: Int, renderable: Renderable) {
        DrawContextUtils.translated(xOffsets[columnIndex].toFloat(), 0f, 0f) {
            renderable.renderXYAligned(
                mouseOffsetX + xOffsets[columnIndex],
                mouseOffsetY + renderY,
                xOffsets[columnIndex + 1] - xOffsets[columnIndex] - xSpacing,
                yOffsets[rowIndex + 1] - yOffsets[rowIndex] - ySpacing,
            )
        }
    }

    override fun renderRow(mouseOffsetX: Int, mouseOffsetY: Int, rowIndex: Int, row: List<Renderable>) {
        super.renderRow(mouseOffsetX, mouseOffsetY, rowIndex, row)
        val yShift = yOffsets[rowIndex + 1] - yOffsets[rowIndex]
        DrawContextUtils.translate(0f, yShift.toFloat(), 0f)
        renderY += yShift
    }

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        scroll.update(isHovered(mouseOffsetX, mouseOffsetY) && shouldAllowLink(true, bypassChecks))

        renderY = 0
        if (hasHeader) renderRow(mouseOffsetX, mouseOffsetY, 0, header)

        val posts = if (hasHeader) yOffsets.drop(1) else yOffsets

        val range = getRange(posts)

        if (hasHeader)
            for (rowIndex in range) renderRow(mouseOffsetX, mouseOffsetY, rowIndex + 1, content[rowIndex])
        else
            for (rowIndex in range) renderRow(mouseOffsetX, mouseOffsetY, rowIndex, content[rowIndex])

        DrawContextUtils.translate(0f, -renderY.toFloat(), 0f)
    }

    companion object {
        fun Renderable.Companion.scrollTable(
            content: List<List<Renderable>>,
            height: Int,
            scrollValue: ScrollValue = ScrollValue(),
            velocity: Double = 2.0,
            button: Int? = null,
            xSpacing: Int = 1,
            ySpacing: Int = 0,
            header: List<Renderable> = emptyList(),
            bypassChecks: Boolean = false,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = ScrollTable(
            content = content,
            height = height,
            scrollValue = scrollValue,
            velocity = velocity,
            button = button,
            xSpacing = xSpacing,
            ySpacing = ySpacing,
            header = header,
            bypassChecks = bypassChecks,
            horizontalAlign = horizontalAlign,
            verticalAlign = verticalAlign,
        )
    }
}
