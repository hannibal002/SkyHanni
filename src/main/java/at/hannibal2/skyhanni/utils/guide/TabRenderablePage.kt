package at.hannibal2.skyhanni.utils.guide

import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.empty

@Suppress("AbstractClassCanBeConcreteClass")
abstract class TabRenderablePage(
    val paddingX: Int = 0,
    val paddingY: Int = 0,
) : TabPage {

    protected var renderable: Renderable? = null

    final override fun buildRenderable(): Renderable {
        val r = renderable ?: return Renderable.empty()
        return object : Renderable {
            override val width = r.width
            override val height = r.height
            override val horizontalAlign = r.horizontalAlign
            override val verticalAlign = r.verticalAlign
            override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
                DrawContextUtils.translate(paddingX.toFloat(), paddingY.toFloat())
                r.render(mouseOffsetX + paddingX, mouseOffsetY + paddingY)
                DrawContextUtils.translate(-paddingX.toFloat(), -paddingY.toFloat())
            }
        }
    }

    override fun onLeave() {
        renderable = null
    }
}
