package at.hannibal2.skyhanni.utils.renderables.container

import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.renderables.Renderable

abstract class ContainerRenderable(
    var renderables: Collection<Renderable>,
    open val spacing: Int = 0,
    override val horizontalAlign: HorizontalAlignment,
    override val verticalAlign: VerticalAlignment
) : Renderable {
    abstract override val width: Int
    abstract override val height: Int
    abstract override fun render(mouseOffsetX: Int, mouseOffsetY: Int)
}

