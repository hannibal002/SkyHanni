package at.hannibal2.skyhanni.features.garden.fortuneguide.pages

import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFStats
import at.hannibal2.skyhanni.features.garden.fortuneguide.FarmingItemType
import at.hannibal2.skyhanni.features.garden.fortuneguide.FortuneStats
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.split
import at.hannibal2.skyhanni.utils.guide.GuideTablePage
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical

class CropPage(val crop0: () -> CropType, sizeX: Int, sizeY: Int, paddingX: Int = 15, paddingY: Int = 7) :
    GuideTablePage(
        sizeX, sizeY, paddingX, paddingY,
    ) {

    val crop get() = crop0()

    override fun onEnter() {
        val item = crop.farmingItem
        FFStats.getCropStats(crop, item.getItemOrNull())

        FarmingItemType.resetClickState()
        val toolLines = toolLines().split().map { Renderable.vertical(it, 2) }
        update(
            listOf(
                header(),
                listOf(
                    toolLines[0],
                    equipDisplay(),
                    toolLines[1],
                ),
            ),
            emptyList(),
        )
    }

    private fun header(): List<Renderable> = buildList {
        add(FortuneStats.BASE.getFarmingBar())
        add(FortuneStats.CROP_TOTAL.getFarmingBar(110))
        add(FortuneStats.CROP_UPGRADE.getFarmingBar())
    }

    private fun FortuneStats.getFarmingBar(
        width: Int = 90,
    ) = Renderable.clickable(
        GuiRenderUtils.getFarmingBar(label(crop), tooltip(crop), current, max, width),
        { onClick(crop) },
    )

    private fun toolLines(): List<Renderable> =
        FortuneStats.entries.filter { it.isActive() && it !in headers }.map { it.getFarmingBar() }

    private fun equipDisplay(): Renderable = with(Renderable) {
        fixedSizeColumn(
            vertical(
                crop.farmingItem.getDisplay(),
                horizontal(
                    vertical(FarmingItemType.getArmorDisplay(), 2),
                    vertical(FarmingItemType.getEquipmentDisplay(), 2),
                    spacing = 2,
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                ),
                horizontal(FarmingItemType.getPetsDisplay(true), 2),
                spacing = 2,
                verticalAlign = RenderUtils.VerticalAlignment.BOTTOM,
            ),
            height = sizeY - 36,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            verticalAlign = RenderUtils.VerticalAlignment.BOTTOM,
        )
    }

    companion object {
        private val headers = setOf(FortuneStats.BASE, FortuneStats.CROP_TOTAL, FortuneStats.CROP_UPGRADE)
    }
}
