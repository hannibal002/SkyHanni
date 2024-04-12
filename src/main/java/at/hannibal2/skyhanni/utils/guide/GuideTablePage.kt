package at.hannibal2.skyhanni.utils.guide

import at.hannibal2.skyhanni.utils.CollectionUtils.tableStretchXPadding
import at.hannibal2.skyhanni.utils.CollectionUtils.tableStretchYPadding
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.renderer.GlStateManager

abstract class GuideTablePage(
    val sizeX: Int,
    val sizeY: Int,
    val paddingX: Int = 0,
    val paddingY: Int = 0,
    val footerSpacing: Int = 2
) : GuidePage() {

    private var renderable: Renderable? = null

    fun update(
        content: List<List<Renderable>>,
        footer: List<Renderable> = emptyList()
    ) {
        val ySpace = (content + listOf(footer)).tableStretchYPadding(sizeY - paddingY * 2)
        renderable =
            Renderable.verticalContainer(
                listOf(
                    Renderable.table(
                        content,
                        xPadding = content.tableStretchXPadding(sizeX - paddingX * 2),
                        yPadding = ySpace
                    ),
                    Renderable.horizontalContainer(footer, footerSpacing, horizontalAlign = HorizontalAlignment.CENTER)
                ), spacing = ySpace
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
