package at.hannibal2.skyhanni.utils.guide

import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.tableStretchXPadding
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import at.hannibal2.skyhanni.utils.renderables.container.table.ScrollTable.Companion.scrollTable

abstract class GuideScrollPage(
    val sizeX: Int,
    val sizeY: Int,
    paddingX: Int = 0,
    paddingY: Int = 0,
    val marginY: Int = 5,
    val velocity: Double = 3.0,
) : GuideRenderablePage(paddingX, paddingY) {

    private val scroll = ScrollValue()

    fun update(header: List<Renderable>, content: List<List<Renderable>>) {
        renderable = Renderable.scrollTable(
            content = content,
            height = sizeY - paddingY * 2,
            scrollValue = scroll,
            velocity = velocity,
            xSpacing = content.tableStretchXPadding(sizeX - paddingX * 2),
            ySpacing = marginY,
            header = header,
            button = 0,
        )
    }
}
