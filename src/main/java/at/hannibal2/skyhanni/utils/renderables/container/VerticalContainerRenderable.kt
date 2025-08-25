package at.hannibal2.skyhanni.utils.renderables.container

import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable1dContainerContext
import at.hannibal2.skyhanni.utils.renderables.RenderableContext
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXAligned

open class VerticalContainerRenderable protected constructor(
    final override var renderables: Collection<Renderable>,
    final override val spacing: Int = 0,
    override val horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
    override val verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.TOP,
) : ContainerRenderable() {

    override val width = renderables.maxOfOrNull { it.width } ?: 0

    override val height = renderables.sumOf { it.height } + spacing * (renderables.size - 1)

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        var y = mouseOffsetY
        renderables.forEach {
            it.renderXAligned(mouseOffsetX, y, width)
            y += it.height + spacing
            DrawContextUtils.translate(0f, (it.height + spacing).toFloat(), 0f)
        }
        DrawContextUtils.translate(0f, (-height - spacing).toFloat(), 0f)
    }

    companion object {

        fun RenderableContext.vertical(
            spacing: Int = 0,
            horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
            verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.TOP,
            entries: Renderable1dContainerContext.() -> Unit,
        ) = VerticalContainerRenderable(Renderable1dContainerContext.result(entries), spacing, horizontalAlign, verticalAlign)

        /**
         * Consider using the [Renderable1dContainerContext] version when possible
         */
        fun RenderableContext.vertical(
            renderables: Collection<Renderable>,
            spacing: Int = 0,
            horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
            verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.TOP,
        ) = VerticalContainerRenderable(renderables, spacing, horizontalAlign, verticalAlign)

        @Deprecated("Use Renderable1dContainerContext version")
        fun RenderableContext.vertical(
            vararg renderables: Renderable,
            spacing: Int = 0,
            horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
            verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.TOP,
        ) = VerticalContainerRenderable(renderables.asList(), spacing, horizontalAlign, verticalAlign)
    }
}
