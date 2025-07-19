package at.hannibal2.skyhanni.utils.renderables.container

import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderYAligned


class HorizontalContainerRenderable private constructor(
    renderables: Collection<Renderable>,
    spacing: Int = 0,
    horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
    verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.TOP,
) : ContainerRenderable(renderables, spacing, horizontalAlign, verticalAlign) {

    override val width = renderables.sumOf { it.width } + spacing * (renderables.size - 1)

    override val height = renderables.maxOfOrNull { it.height } ?: 0

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        var x = mouseOffsetX
        renderables.forEach {
            it.renderYAligned(x, mouseOffsetY, height)
            x += it.width + spacing
            DrawContextUtils.translate((it.width + spacing).toFloat(), 0f, 0f)
        }
        DrawContextUtils.translate((-width - spacing).toFloat(), 0f, 0f)
    }

    companion object {
        fun Renderable.Companion.horizontal(
            renderables: Collection<Renderable>,
            spacing: Int = 0,
            horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
            verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.TOP,
        ) = HorizontalContainerRenderable(renderables, spacing, horizontalAlign, verticalAlign)

        fun Renderable.Companion.horizontal(
            spacing: Int = 0,
            horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
            verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.TOP,
            builderAction: MutableList<Renderable>.() -> Unit,
        ) = HorizontalContainerRenderable(buildList { builderAction() }, spacing, horizontalAlign, verticalAlign)

        fun Renderable.Companion.horizontal(
            vararg renderables: Renderable,
            spacing: Int = 0,
            horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
            verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.TOP,
        ) = HorizontalContainerRenderable(renderables.asList(), spacing, horizontalAlign, verticalAlign)
    }
}
