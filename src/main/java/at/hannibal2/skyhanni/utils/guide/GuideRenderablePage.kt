package at.hannibal2.skyhanni.utils.guide

import at.hannibal2.skyhanni.utils.compat.DrawContext
import at.hannibal2.skyhanni.utils.renderables.Renderable

abstract class GuideRenderablePage(
    val paddingX: Int = 0,
    val paddingY: Int = 0
) : GuidePage() {

    protected var renderable: Renderable? = null
    var drawContext = DrawContext()

    final override fun drawPage(mouseX: Int, mouseY: Int) {
        drawContext.getMatrices().translate(paddingX.toFloat(), paddingY.toFloat(), 0f)
        renderable?.render(paddingX, paddingY)
        drawContext.getMatrices().translate(-paddingX.toFloat(), -paddingY.toFloat(), 0f)
    }

    override fun onLeave() {
        renderable = null
    }

}
