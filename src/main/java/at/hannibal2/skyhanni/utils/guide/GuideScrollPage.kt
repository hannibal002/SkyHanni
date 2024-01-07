package at.hannibal2.skyhanni.utils.guide

import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.renderer.GlStateManager

abstract class GuideScrollPage(
    val sizeX: Int,
    val sizeY: Int,
    val paddingX: Int = 0,
    val paddingY: Int = 0,
    val hasHeader: Boolean = true,
) : GuidePage() {

    private var renderable: Renderable? = null

    fun update(content: List<List<Renderable?>>) {
        renderable = Renderable.scrollTable(
            content = content,
            height = sizeY - paddingY * 2,
            velocity = 1.0,
            xPadding = Renderable.calculateStretchXPadding(content, sizeX - paddingX * 3),
            yPadding = 5,
            hasHeader = hasHeader,
            button = 0
        )
    }

    override fun drawPage(mouseX: Int, mouseY: Int) {
        GlStateManager.translate(paddingX.toFloat(), paddingY.toFloat(), 0f)
        Renderable.withMousePosition(mouseX, mouseY) {
            renderable?.render(paddingX, paddingY)
        }
        GlStateManager.translate(-paddingX.toFloat(), -paddingY.toFloat(), 0f)
    }
}
