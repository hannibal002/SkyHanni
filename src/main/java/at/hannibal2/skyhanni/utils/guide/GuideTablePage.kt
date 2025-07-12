package at.hannibal2.skyhanni.utils.guide

import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.tableStretchXPadding
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.tableStretchYPadding
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable

abstract class GuideTablePage(
    val sizeX: Int,
    val sizeY: Int,
    paddingX: Int = 0,
    paddingY: Int = 0,
    val footerSpacing: Int = 2,
) : GuideRenderablePage(paddingX, paddingY) {

    fun update(
        content: List<List<Renderable>>,
        footer: List<Renderable> = emptyList(),
    ) {
        val ySpace = (content + listOf(footer)).tableStretchYPadding(sizeY - paddingY * 2)
        renderable =
            VerticalContainerRenderable(
                listOf(
                    Renderable.table(
                        content,
                        xPadding = content.tableStretchXPadding(sizeX - paddingX * 2),
                        yPadding = ySpace,
                    ),
                    HorizontalContainerRenderable(footer, footerSpacing, horizontalAlign = HorizontalAlignment.CENTER),
                ),
                spacing = ySpace,
            )
    }
}
