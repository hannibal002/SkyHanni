package at.hannibal2.skyhanni.utils.renderables.container

import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable1dContainerContext
import at.hannibal2.skyhanni.utils.renderables.RenderableContext
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderYAligned


class HorizontalContainerRenderable private constructor(
    override val renderables: Collection<Renderable>,
    override val spacing: Int = 0,
    override val horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
    override val verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.TOP,
) : ContainerRenderable() {

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

        fun RenderableContext.horizontal(
            spacing: Int = 0,
            horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
            verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.TOP,
            entries: Renderable1dContainerContext.() -> Unit,
        ) = HorizontalContainerRenderable(Renderable1dContainerContext.result(entries), spacing, horizontalAlign, verticalAlign)

        /**
         * Consider using the [Renderable1dContainerContext] version when possible
         */
        fun RenderableContext.horizontal(
            renderables: Collection<Renderable>,
            spacing: Int = 0,
            horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
            verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.TOP,
        ) = HorizontalContainerRenderable(renderables, spacing, horizontalAlign, verticalAlign)

        @Deprecated("Use Renderable1dContainerContext version")
        fun RenderableContext.horizontal(
            vararg renderables: Renderable,
            spacing: Int = 0,
            horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
            verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.TOP,
        ) = HorizontalContainerRenderable(renderables.asList(), spacing, horizontalAlign, verticalAlign)
    }
}
