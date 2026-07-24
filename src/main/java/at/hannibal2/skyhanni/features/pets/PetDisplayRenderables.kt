package at.hannibal2.skyhanni.features.pets

import at.hannibal2.skyhanni.config.features.pets.display.visual.PetItemConfig
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.CircularRenderable
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import io.github.notenoughupdates.moulconfig.ChromaColour
import java.awt.Color
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.roundToInt

internal data class AnchoredRenderable(
    val renderable: Renderable,
    val anchorX: Int,
    val anchorY: Int,
    val anchorWidth: Int,
    val anchorHeight: Int,
)

internal data class VisualIconLayer(val renderable: Renderable, val backgroundEnabled: Boolean)

internal data class ExpSharePetState(val petData: PetData, val disabled: Boolean)

internal fun Renderable.anchorToSelf() = AnchoredRenderable(
    renderable = this,
    anchorX = 0,
    anchorY = 0,
    anchorWidth = width,
    anchorHeight = height,
)

internal fun ChromaColour.withOpacity(opacity: Float): ChromaColour {
    val color = toColor()
    val alpha = (color.alpha * opacity).roundToInt().coerceIn(0, 255)
    return color.toChromaColor(alpha)
}

internal fun buildCircularContainer(
    root: Renderable,
    backgroundColor: ChromaColour,
    filledPercentage: Double = 100.0,
    unfilledColor: ChromaColour = Color.LIGHT_GRAY.toChromaColor(255),
    padding: Int = 2,
    preview: Boolean,
): Renderable = if (preview) {
    PreviewCircularContainerRenderable(root, backgroundColor, filledPercentage, unfilledColor, padding)
} else {
    PetDisplayCircularContainerRenderable(root, backgroundColor, filledPercentage, unfilledColor, padding)
}

internal fun Renderable.wrapInPetItemOrSelf(
    enabled: Boolean,
    petData: PetData,
    petItemConfig: PetItemConfig,
    opacity: Float = 1.0f,
): Renderable = if (!enabled) this else petData.heldItemInternalName?.getItemStackOrNull()?.let {
    PetItemOverlayRenderable(
        root = this,
        item = Renderable.item(it) {
            scale = petItemConfig.scale.get().toDouble()
            alpha = opacity
        },
        placement = petItemConfig.placement.get(),
    )
} ?: this

private class PetItemOverlayRenderable(
    private val root: Renderable,
    private val item: Renderable,
    private val placement: PetItemConfig.PetItemPlacement,
) : Renderable {
    override val width = root.width
    override val height = root.height
    override val horizontalAlign = root.horizontalAlign
    override val verticalAlign = root.verticalAlign

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        root.render(mouseOffsetX, mouseOffsetY)
        val itemX = anchorX() - item.width / 2
        val itemY = anchorY() - item.height / 2
        DrawContextUtils.translated(itemX, itemY) {
            item.render(mouseOffsetX + itemX, mouseOffsetY + itemY)
        }
    }

    private fun anchorX(): Int = when (placement.horizontal) {
        RenderUtils.HorizontalAlignment.LEFT -> 0
        RenderUtils.HorizontalAlignment.CENTER -> width / 2
        RenderUtils.HorizontalAlignment.RIGHT -> width
        RenderUtils.HorizontalAlignment.DONT_ALIGN -> width / 2
    }

    private fun anchorY(): Int = when (placement.vertical) {
        RenderUtils.VerticalAlignment.TOP -> 0
        RenderUtils.VerticalAlignment.CENTER -> height / 2
        RenderUtils.VerticalAlignment.BOTTOM -> height
        RenderUtils.VerticalAlignment.DONT_ALIGN -> height / 2
    }
}

private class PreviewCircularContainerRenderable(
    private val root: Renderable,
    private val backgroundColor: ChromaColour,
    private val filledPercentage: Double,
    private val unfilledColor: ChromaColour,
    private val padding: Int,
) : Renderable {
    private val radius = (max(root.width, root.height) / 2) + padding
    private val takenSpace = 2 * (radius - padding)
    override val width = radius * 2
    override val height = radius * 2
    override val horizontalAlign = root.horizontalAlign
    override val verticalAlign = root.verticalAlign
    private val circleSegments = buildCircleSegments()

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        circleSegments.forEach { it.render() }
        DrawContextUtils.translated(padding.toFloat(), padding.toFloat()) {
            root.renderCentered(mouseOffsetX + padding, mouseOffsetY + padding, takenSpace, takenSpace)
        }
    }

    private fun buildCircleSegments(): List<CircleSegment> {
        if (filledPercentage >= 100.0) {
            return buildCirclePixels(backgroundColor.toColor().rgb) { _, _ -> true }
        }

        val baseAngle = Math.PI.toFloat() * 3f / 2f
        val endAngle = (baseAngle + ((100.0 - filledPercentage) / 50.0 * Math.PI).toFloat())
            .mod(2f * Math.PI.toFloat())
        return buildCircleArc(backgroundColor.toColor().rgb, baseAngle, endAngle) +
            buildCircleArc(unfilledColor.toColor().rgb, endAngle, baseAngle)
    }

    private fun buildCircleArc(color: Int, startAngle: Float, endAngle: Float): List<CircleSegment> {
        return buildCirclePixels(color) { dx, dy ->
            angleBetween(
                atan2(-dy, -dx).toFloat().mod(TAU),
                startAngle,
                endAngle,
            )
        }
    }

    private fun buildCirclePixels(color: Int, includeSample: (Double, Double) -> Boolean): List<CircleSegment> = buildList {
        val radiusSquared = radius.toDouble() * radius
        val alpha = color ushr 24 and 0xFF
        val rgb = color and 0x00FFFFFF
        for (y in 0 until width) {
            var runStart: Int? = null
            var runColor = 0
            for (x in 0 until width) {
                var samplesInside = 0
                for (sampleY in SAMPLE_OFFSETS) {
                    for (sampleX in SAMPLE_OFFSETS) {
                        val dx = x + sampleX - radius
                        val dy = y + sampleY - radius
                        if (dx * dx + dy * dy <= radiusSquared && includeSample(dx, dy)) samplesInside++
                    }
                }

                val sampleColor = if (samplesInside > 0) {
                    ((alpha * samplesInside / SAMPLE_COUNT) shl 24) or rgb
                } else 0

                if (sampleColor == runColor) continue
                runStart?.let { add(CircleSegment(it, y, x, runColor)) }
                runStart = if (sampleColor == 0) null else x
                runColor = sampleColor
            }
            runStart?.let {
                add(CircleSegment(it, y, width, runColor))
            }
        }
    }

    private data class CircleSegment(
        val startX: Int,
        val y: Int,
        val endX: Int,
        val color: Int,
    ) {
        fun render() {
            GuiRenderUtils.drawRect(startX, y, endX, y + 1, color)
        }
    }

    private fun angleBetween(angle: Float, startAngle: Float, endAngle: Float): Boolean {
        return if (startAngle <= endAngle) {
            angle in startAngle..endAngle
        } else {
            angle >= startAngle || angle <= endAngle
        }
    }

    private companion object {
        private val SAMPLE_OFFSETS = DoubleArray(4) { (it + 0.5) / 4.0 }
        private const val SAMPLE_COUNT = 16
        private val TAU = (2.0 * Math.PI).toFloat()
    }
}

private class PetDisplayCircularContainerRenderable(
    private val root: Renderable,
    backgroundColor: ChromaColour,
    filledPercentage: Double = 100.0,
    unfilledColor: ChromaColour = Color.LIGHT_GRAY.toChromaColor(255),
    private val padding: Int = 2,
) : CircularRenderable(
    backgroundColor,
    radius = (max(root.width, root.height) / 2) + padding,
    1f,
    filledPercentage,
    unfilledColor,
    root.horizontalAlign,
    root.verticalAlign,
) {
    private val takenSpace = 2 * (radius - padding)

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        super.render(mouseOffsetX, mouseOffsetY)
        DrawContextUtils.translated(padding.toFloat(), padding.toFloat()) {
            root.renderCentered(mouseOffsetX + padding, mouseOffsetY + padding, takenSpace, takenSpace)
        }
    }
}

private fun Renderable.renderCentered(mouseOffsetX: Int, mouseOffsetY: Int, xSpace: Int, ySpace: Int) {
    val xOffset = (xSpace - width) / 2f
    val yOffset = (ySpace - height) / 2f
    DrawContextUtils.translated(xOffset, yOffset) {
        render(mouseOffsetX + xOffset.roundToInt(), mouseOffsetY + yOffset.roundToInt())
    }
}
