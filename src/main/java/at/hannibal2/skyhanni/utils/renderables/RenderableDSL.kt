package at.hannibal2.skyhanni.utils.renderables

import kotlin.collections.plusAssign

@DslMarker
annotation class RenderableDSL

@RenderableDSL
interface RenderableContext

// "buildList" version of an Entry Point
interface RenderableContainerContext<T> : RenderableContext {
    val result: T
    operator fun Renderable.unaryPlus()
    // The suppression is needed since there is a bug/is not implemented feature in the compiler, as it does not work on everything but here it should work.
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("unaryPlusRenderableNullable")
    operator fun Renderable?.unaryPlus() = this?.unaryPlus() ?: Unit
}

// 1d implementation (only needs to be defined once and all containers can use it)
class Renderable1dContainerContext : RenderableContainerContext<List<Renderable>> {
    private val entries = mutableListOf<Renderable>()

    override val result: List<Renderable> get() = entries

    override operator fun Renderable.unaryPlus() {
        entries += this
    }

    companion object {
        fun result(entries: Renderable1dContainerContext.() -> Unit) = Renderable1dContainerContext().apply(entries).result
    }
}

// 2d implementation (only needs to be defined once and all containers can use it)
class Renderable2dContainerContext : RenderableContainerContext<List<List<Renderable>>> {
    private val entries = mutableListOf<List<Renderable>>()

    override val result: List<List<Renderable>> get() = entries

    override operator fun Renderable.unaryPlus() {
        entries += listOf(this)
    }

    fun row(block: Renderable1dContainerContext.() -> Unit) {
        entries.add(Renderable1dContainerContext().apply(block).result)
    }

    companion object {
        fun result(entries: Renderable2dContainerContext.() -> Unit) = Renderable2dContainerContext().apply(entries).result
    }
}
