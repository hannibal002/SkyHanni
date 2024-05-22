package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import kotlin.math.ceil

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

    private val inventoryTextures by lazy { ResourceLocation("skyhanni", "gui/slot.png") }
    private const val TEXTURE_WIDTH = 90
    private const val TEXTURE_HEIGHT = 54

    private enum class SlotsUv(val uMin: Int, val uMax: Int, val vMin: Int, val vMax: Int) {
        TOP_LEFT_CORNER(14, 18, 32, 36),
        TOP_BORDER(18, 36, 32, 36),
        TOP_RIGHT_CORNER(36, 40, 32, 36),
        LEFT_BORDER(68, 72, 18, 36),
        CENTER(0, 18, 0, 18),
        RIGHT_BORDER(72, 76, 18, 36),
        BOTTOM_LEFT_CORNER(14, 18, 36, 40),
        BOTTOM_BORDER(18, 36, 36, 40),
        BOTTOM_RIGHT_CORNER(36, 40, 36, 40),
        HALF_EMPTY(54, 63, 0, 18),
        EMPTY(18, 36, 0, 18),
        ;

        fun getUvCoords(): FloatArray {
            return floatArrayOf(
                (uMin.toFloat() / TEXTURE_WIDTH),
                (uMax.toFloat() / TEXTURE_WIDTH),
                (vMin.toFloat() / TEXTURE_HEIGHT),
                (vMax.toFloat() / TEXTURE_HEIGHT),
            )
        }

        fun height() = vMax - vMin
        fun width() = uMax - uMin
    }

    private fun createUvList(size: Int, maxSize: Int) = buildList {
        val length = if (size >= maxSize) maxSize else size
        val lastLength = if (size % maxSize == 0) maxSize else size % maxSize
        val isWeird = lastLength % 2 != length % 2

        val rows = ceil(size.toDouble() / maxSize).toInt()

        for (i in 0 until rows + 2) {
            val row = mutableListOf<SlotsUv>()
            if (i == 0) { // top border
                row.add(SlotsUv.TOP_LEFT_CORNER)
                row.addAll(List(length) { SlotsUv.TOP_BORDER })
                row.add(SlotsUv.TOP_RIGHT_CORNER)
            } else if (i == rows + 1) { // last border
                row.add(SlotsUv.BOTTOM_LEFT_CORNER)
                row.addAll(List(length) { SlotsUv.BOTTOM_BORDER })
                row.add(SlotsUv.BOTTOM_RIGHT_CORNER)
            } else if (i == rows && rows != 1) { // last row
                val spaces = if (isWeird) (length - lastLength) - 1 else length - lastLength
                row.add(SlotsUv.LEFT_BORDER)
                row.addAll(List(spaces / 2) { SlotsUv.EMPTY })
                if (isWeird) row.add(SlotsUv.HALF_EMPTY)
                row.addAll(List(lastLength) { SlotsUv.CENTER })
                if (isWeird) row.add(SlotsUv.HALF_EMPTY)
                row.addAll(List(spaces / 2) { SlotsUv.EMPTY })
                row.add(SlotsUv.RIGHT_BORDER)
            } else {
                row.add(SlotsUv.LEFT_BORDER)
                row.addAll(List(length) { SlotsUv.CENTER })
                row.add(SlotsUv.RIGHT_BORDER)
            }
            add(row.toList())
        }
    }

    fun createFakeInventory(items: List<ItemStack?>, maxRowSize: Int, scale: Double): Renderable {
        val uvList = createUvList(items.size, maxRowSize)
        val finalList = mutableListOf<List<Renderable>>()
        var index = 0
        uvList.forEach { uvRow ->
            val row = mutableListOf<Renderable>()
            uvRow.forEach { uv ->
                val uvArray = uv.getUvCoords()
                val renderable = if (uv == SlotsUv.CENTER) {
                    (items[index]?.let { item ->
                        Renderable.itemStack(
                            item,
                            scale,
                            0,
                            0
                        )
                    } ?: Renderable.placeholder((16 * scale).toInt(), (16 * scale).toInt())).also { index++ }
                } else Renderable.placeholder(0, 0)
                row.add(
                    Renderable.drawInsideFixedSizedImage(
                        renderable,
                        inventoryTextures,
                        (uv.width() * scale).toInt(),
                        (uv.height() * scale).toInt(),
                        padding = scale.toInt(),
                        uMin = uvArray[0],
                        uMax = uvArray[1],
                        vMin = uvArray[2],
                        vMax = uvArray[3],
                    )
                )
            }
            finalList.add(row)
        }

        return Renderable.verticalContainer(finalList.map { Renderable.horizontalContainer(it, 0) }, 0)
    }
}
