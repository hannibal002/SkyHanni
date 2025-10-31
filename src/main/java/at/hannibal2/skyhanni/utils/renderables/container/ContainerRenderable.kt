package at.hannibal2.hanni.utils.renderables.container

import at.hannibal2.hanni.utils.renderables.Renderable

abstract class ContainerRenderable : Renderable {
    abstract val renderables: Collection<Renderable>
    open val spacing: Int = 0
}

