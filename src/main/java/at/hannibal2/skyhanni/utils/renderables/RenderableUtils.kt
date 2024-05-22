package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import net.minecraft.client.renderer.GlStateManager

internal object RenderableUtils {

    /** Calculates the absolute x position of the columns in a table*/
    fun calculateTableXOffsets(content: List<List<Renderable?>>, xPadding: Int) = run {
        var buffer = 0
        var index = 0
        buildList {
            add(0)
            while (true) {
                buffer += content.map { it.getOrNull(index) }.takeIf { it.any { it != null } }?.maxOf {
                    it?.width ?: 0
                }?.let { it + xPadding } ?: break
                add(buffer)
                index++
            }
        }
    }

    /** Calculates the absolute y position of the rows in a table*/
    fun calculateTableYOffsets(content: List<List<Renderable?>>, yPadding: Int) = run {
        var buffer = 0
        listOf(0) + content.map { row ->
            buffer += row.maxOf { it?.height ?: 0 } + yPadding
            buffer
        }
    }

    private fun calculateAlignmentXOffset(renderable: Renderable, xSpace: Int) = when (renderable.horizontalAlign) {
        HorizontalAlignment.LEFT -> 0
        HorizontalAlignment.CENTER -> (xSpace - renderable.width) / 2
        HorizontalAlignment.RIGHT -> xSpace - renderable.width
        else -> 0
    }

    private fun calculateAlignmentYOffset(renderable: Renderable, ySpace: Int) = when (renderable.verticalAlign) {
        VerticalAlignment.TOP -> 0
        VerticalAlignment.CENTER -> (ySpace - renderable.height) / 2
        VerticalAlignment.BOTTOM -> ySpace - renderable.height
        else -> 0
    }

    fun Renderable.renderXYAligned(posX: Int, posY: Int, xSpace: Int, ySpace: Int) {
        val xOffset = calculateAlignmentXOffset(this, xSpace)
        val yOffset = calculateAlignmentYOffset(this, ySpace)
        GlStateManager.translate(xOffset.toFloat(), yOffset.toFloat(), 0f)
        this.render(posX + xOffset, posY + yOffset)
        GlStateManager.translate(-xOffset.toFloat(), -yOffset.toFloat(), 0f)
    }

    fun Renderable.renderXAligned(posX: Int, posY: Int, xSpace: Int) {
        val xOffset = calculateAlignmentXOffset(this, xSpace)
        GlStateManager.translate(xOffset.toFloat(), 0f, 0f)
        this.render(posX + xOffset, posY)
        GlStateManager.translate(-xOffset.toFloat(), 0f, 0f)
    }

    fun Renderable.renderYAligned(posX: Int, posY: Int, ySpace: Int) {
        val yOffset = calculateAlignmentYOffset(this, ySpace)
        GlStateManager.translate(0f, yOffset.toFloat(), 0f)
        this.render(posX, posY + yOffset)
        GlStateManager.translate(0f, -yOffset.toFloat(), 0f)
    }
}

internal abstract class RenderableWrapper internal constructor(protected val content: Renderable) : Renderable {
    override val width = content.width
    override val height = content.height
    override val horizontalAlign = content.horizontalAlign
    override val verticalAlign = content.verticalAlign
}
