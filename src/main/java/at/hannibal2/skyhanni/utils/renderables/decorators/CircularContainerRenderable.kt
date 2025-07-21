package at.hannibal2.skyhanni.utils.renderables.decorators

import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.primitives.CircularRenderable
import io.github.notenoughupdates.moulconfig.ChromaColour
import java.awt.Color
import kotlin.math.max

class CircularContainerRenderable(
    override val root: Renderable,
    backgroundColor: ChromaColour,
    smoothness: Float = 1f,
    filledPercentage: Double = 100.0,
    unfilledColor: ChromaColour = Color.LIGHT_GRAY.toChromaColor(255),
    private val padding: Int = 2,
) : CircularRenderable(
    backgroundColor,
    radius = (max(root.width, root.height) / 2) + padding,
    smoothness,
    filledPercentage,
    unfilledColor,
    root.horizontalAlign,
    root.verticalAlign,
),
    RenderableDecorator {
    private val takenSpace = 2 * (radius - padding)
    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        super.render(mouseOffsetX, mouseOffsetY)
        DrawContextUtils.translated(padding.toFloat(), padding.toFloat(), 0f) {
            root.renderXYAligned(mouseOffsetX + padding, mouseOffsetY + padding, takenSpace, takenSpace)
        }
    }

    companion object {
        fun Renderable.Companion.circularContainer(
            root: Renderable,
            backgroundColor: ChromaColour,
            smoothness: Float = 1f,
            filledPercentage: Double = 100.0,
            unfilledColor: ChromaColour = Color.LIGHT_GRAY.toChromaColor(255),
            padding: Int = 2,
        ) = CircularContainerRenderable(root, backgroundColor, smoothness, filledPercentage, unfilledColor, padding)
    }
}
