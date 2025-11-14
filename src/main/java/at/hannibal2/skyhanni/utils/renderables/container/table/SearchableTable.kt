package at.hannibal2.skyhanni.utils.renderables.container.table

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.filterListMap
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned

class SearchableTable private constructor(
    rawContent: Map<List<Renderable>, String>,
    textInput: TextInput,
    key: Int,
    override val xSpacing: Int = 1,
    override val ySpacing: Int = 0,
    val header: List<Renderable> = emptyList(),
    useEmptySpace: Boolean = false,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
) : TabularRenderableWithRowRender<List<Renderable>, Set<List<Renderable>>> {

    override var content = filterListMap(rawContent, textInput.textBox)
    private val fullContent = if (header.isNotEmpty()) listOf(header) + rawContent.keys else rawContent.keys

    private val xOffsets = RenderableUtils.calculateTableX(fullContent, xSpacing)
    private val yOffsets: Map<List<Renderable?>, Int> = RenderableUtils.calculateTableY(fullContent, ySpacing)

    private val emptySpaceX = if (useEmptySpace) 0 else xSpacing
    private val emptySpaceY = if (useEmptySpace) 0 else ySpacing

    override val width = xOffsets.sum() - emptySpaceX
    override val height = yOffsets.sumAllValues().toInt() - emptySpaceY

    init {
        textInput.registerToEvent(key) {
            content = filterListMap(rawContent, textInput.textBox)
        }
    }

    private var renderY = 0

    override fun renderRow(mouseOffsetX: Int, mouseOffsetY: Int, rowIndex: Int, row: List<Renderable>) {
        var renderX = 0
        val yShift = yOffsets[row] ?: row.firstOrNull()?.height ?: 0
        for ((index, renderable) in row.withIndex()) {
            val xShift = xOffsets[index]
            renderable.renderXYAligned(
                mouseOffsetX + renderX,
                mouseOffsetY + renderY,
                xShift - emptySpaceX,
                yShift - emptySpaceY,
            )
            DrawContextUtils.translate(xShift.toFloat(), 0f, 0f)
            renderX += xShift
        }
        DrawContextUtils.translate(-renderX.toFloat(), yShift.toFloat(), 0f)
        renderY += yShift
    }

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        renderY = 0
        if (header.isNotEmpty()) {
            renderRow(mouseOffsetX, mouseOffsetY, -1, header)
        }
        super.render(mouseOffsetX, mouseOffsetY)
        DrawContextUtils.translate(0f, -renderY.toFloat(), 0f)
    }

    companion object {
        fun Renderable.Companion.searchableTable(
            content: Map<List<Renderable>, String>,
            textInput: TextInput,
            key: Int,
            xSpacing: Int = 1,
            ySpacing: Int = 0,
            header: List<Renderable> = emptyList(),
            useEmptySpace: Boolean = false,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = SearchableTable(content, textInput, key, xSpacing, ySpacing, header, useEmptySpace, horizontalAlign, verticalAlign)
    }
}
