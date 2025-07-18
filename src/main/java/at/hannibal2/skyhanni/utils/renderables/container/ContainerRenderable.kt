package at.hannibal2.skyhanni.utils.renderables.container

import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.renderables.Renderable

abstract class ContainerRenderable : Renderable {
    abstract val renderables: Collection<Renderable>
    open val spacing: Int = 0
}

