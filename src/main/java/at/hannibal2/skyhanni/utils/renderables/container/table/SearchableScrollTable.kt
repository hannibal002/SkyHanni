package at.hannibal2.skyhanni.utils.renderables.container.table

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.filterListMap
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.shouldAllowLink
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.ScrollInput
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import at.hannibal2.skyhanni.utils.renderables.container.SlidingWindowWithScrollHints
import at.hannibal2.skyhanni.utils.renderables.container.absoluteProvider
import at.hannibal2.skyhanni.utils.renderables.primitives.text

class SearchableScrollTable private constructor(
    rawContent: Map<List<Renderable>, String?>,
    override val height: Int,
    private val scrollValue: ScrollValue = ScrollValue(),
    private val velocity: Double = 2.0,
    private val button: Int? = null,
    textInput: TextInput,
    key: Int,
    override val xSpacing: Int = 1,
    override val ySpacing: Int = 0,
    private val header: List<Renderable> = emptyList(),
    private val bypassChecks: Boolean = false,
    override val showScrollableTipsInList: Boolean = false,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
) : TabularRenderableWithRowRender<List<Renderable>, List<List<Renderable>>>, SlidingWindowWithScrollHints {

    override var content = filterListMap(rawContent, textInput.textBox).toList()

    private val fullContent = if (header.isNotEmpty()) listOf(header) + rawContent.keys else rawContent.keys

    private val xOffsets = RenderableUtils.calculateTableX(fullContent, xSpacing)
    private val yOffsets: Map<List<Renderable?>, Int> = RenderableUtils.calculateTableY(fullContent, ySpacing)

    override val width = maxOf(xOffsets.sum(), scrollUpTip.width, scrollDownTip.width)

    override val windowMin = (yOffsets[header] ?: 0)
    override val windowSize = height - windowMin
    override var windowMax = createWindowMax()

    private fun createWindowMax() = content.sumOf { yOffsets[it] ?: 0 } + windowMin

    override val scrollUpSize = scrollUpTip.height
    override val scrollDownSize = scrollDownTip.height

    private val getRange = absoluteProvider<List<Renderable>> { yOffsets[it] ?: 0 }

    override var scroll = createScroll()

    private fun createScroll() = ScrollInput.Companion.Vertical(
        scrollValue = scrollValue,
        minHeight = lowerBound,
        maxHeight = upperBound,
        velocity = velocity,
        dragScrollMouseButton = button,
    )

    init {
        textInput.registerToEvent(key) {
            // null = ignored, never filtered
            content = filterListMap(rawContent, textInput.textBox).toList()
            scroll = createScroll()
            windowMax = createWindowMax()
        }
    }

    private var renderY = 0

    override fun renderRow(mouseOffsetX: Int, mouseOffsetY: Int, rowIndex: Int, row: List<Renderable>) {
        var offset = 0
        val yShift = yOffsets[row] ?: 0
        for ((index, renderable) in row.withIndex()) {
            renderable.renderXYAligned(
                mouseOffsetX + offset,
                mouseOffsetY + renderY,
                xOffsets[index],
                yShift,
            )
            DrawContextUtils.translate(xOffsets[index].toFloat(), 0f, 0f)
            offset += xOffsets[index]
        }
        DrawContextUtils.translate(-offset.toFloat(), 0f, 0f)
        DrawContextUtils.translate(0f, yShift.toFloat(), 0f)
        renderY += yShift
    }

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        scroll.update(isHovered(mouseOffsetX, mouseOffsetY) && shouldAllowLink(true, bypassChecks))

        renderY = 0
        if (header.isNotEmpty()) {
            renderRow(mouseOffsetX, mouseOffsetY, 0, header)
        }

        val content = content

        val range = if (content.size == 1) {
            0..0
        } else {
            getRange(content)
        }

        if (showScrollableTipsInList && range.first != 0) {
            scrollUpTip.render(mouseOffsetX, mouseOffsetY)
            val yShift = scrollUpTip.height
            renderY += yShift
            DrawContextUtils.translate(0f, yShift.toFloat(), 0f)
        }

        for (rowIndex in range) {
            val row = content[rowIndex]
            renderRow(mouseOffsetX, mouseOffsetY, rowIndex, row)
        }

        if (showScrollableTipsInList && range.last != content.lastIndex) {
            scrollDownTip.render(mouseOffsetX, mouseOffsetY)
        }

        DrawContextUtils.translate(0f, -renderY.toFloat(), 0f)

    }

    companion object {
        private val scrollUpTip = Renderable.text("§7§oMore items above (scroll)")
        private val scrollDownTip = Renderable.text("§7§oMore items below (scroll)")

        fun Renderable.Companion.searchableScrollTable(
            content: Map<List<Renderable>, String?>,
            height: Int,
            scrollValue: ScrollValue = ScrollValue(),
            velocity: Double = 2.0,
            button: Int? = null,
            textInput: TextInput,
            key: Int,
            xSpacing: Int = 1,
            ySpacing: Int = 0,
            header: List<Renderable> = emptyList(),
            bypassChecks: Boolean = false,
            showScrollableTipsInList: Boolean = false,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = SearchableScrollTable(
            rawContent = content,
            height = height,
            scrollValue = scrollValue,
            velocity = velocity,
            button = button,
            textInput = textInput,
            key = key,
            xSpacing = xSpacing,
            ySpacing = ySpacing,
            header = header,
            bypassChecks = bypassChecks,
            showScrollableTipsInList = showScrollableTipsInList,
            horizontalAlign = horizontalAlign,
            verticalAlign = verticalAlign,
        )
    }
}
