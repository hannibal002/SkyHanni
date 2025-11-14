package at.hannibal2.skyhanni.utils.guide

import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.tableStretchXPadding
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.tableStretchYPadding
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.container.table.TableRenderable.Companion.table

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
        renderable = Renderable.vertical(
            Renderable.table(
                content,
                xSpacing = content.tableStretchXPadding(sizeX - paddingX * 2),
                ySpacing = ySpace,
            ),
            Renderable.horizontal(footer, footerSpacing, horizontalAlign = HorizontalAlignment.CENTER),
            spacing = ySpace,
        )
    }
}
