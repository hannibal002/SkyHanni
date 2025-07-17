package at.hannibal2.skyhanni.utils.renderables.decorators

import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable

interface RenderableDecorator : Renderable {
    val root: Renderable
}

interface RenderableDecoratorOnlyRender : RenderableDecorator {
    override val height: Int get() = root.height
    override val width: Int get() = root.width
    override val horizontalAlign: RenderUtils.HorizontalAlignment get() = root.horizontalAlign
    override val verticalAlign: RenderUtils.VerticalAlignment get() = root.verticalAlign
}
