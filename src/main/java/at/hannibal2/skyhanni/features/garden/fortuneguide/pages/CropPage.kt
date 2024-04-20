package at.hannibal2.skyhanni.features.garden.fortuneguide.pages

import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFStats
import at.hannibal2.skyhanni.features.garden.fortuneguide.FarmingItems
import at.hannibal2.skyhanni.features.garden.fortuneguide.FortuneStats
import at.hannibal2.skyhanni.utils.CollectionUtils.split
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.guide.GuideTablePage
import at.hannibal2.skyhanni.utils.renderables.Renderable

class CropPage(sizeX: Int, sizeY: Int, paddingX: Int = 15, paddingY: Int = 7) : GuideTablePage(
    sizeX,
    sizeY,
    paddingX,
    paddingY
) {

    override fun onEnter() {
        FarmingItems.resetClickState()
        val toolLines = toolLines().split().map { Renderable.verticalContainer(it, 2) }
        update(
            listOf(
                header(),
                listOf(
                    toolLines[0],
                    equipDisplay(),
                    toolLines[1],
                )
            ),
            emptyList()
        )
    }

    private val headers = setOf(FortuneStats.BASE, FortuneStats.CROP_TOTAL, FortuneStats.CROP_UPGRADE)

    private fun header(): List<Renderable> = buildList {
        add((FortuneStats.BASE to FFStats.cropPage[FortuneStats.BASE]!!).getFarmingBar())

        add(with(FortuneStats.CROP_TOTAL) {
            val value = FFStats.cropPage[this]!!
            GuiRenderUtils.getFarmingBar(
                this.label.replace("Crop", FFGuideGUI.currentCrop?.name?.replace("_", " ")?.firstLetterUppercase()!!),
                this.tooltip,
                value.first,
                value.second,
                90
            )
        })
        add((FortuneStats.CROP_UPGRADE to FFStats.cropPage[FortuneStats.CROP_UPGRADE]!!).getFarmingBar())
    }

    private fun Map.Entry<FortuneStats, Pair<Double, Double>>.getFarmingBar() = (key to value).getFarmingBar()

    private fun Pair<FortuneStats, Pair<Double, Double>>.getFarmingBar() = GuiRenderUtils.getFarmingBar(
        first.label, first.tooltip, second.first, second.second,
        90
    )

    private fun toolLines(): List<Renderable> =
        FFStats.cropPage.filter { it.key !in headers }.map { it.getFarmingBar() }

    private fun equipDisplay(): Renderable =
        Renderable.fixedSizeCollum(
            Renderable.verticalContainer(
                listOf(
                    FFGuideGUI.currentCrop?.farmingItem?.getDisplay() ?: Renderable.placeholder(0, 0),
                    Renderable.horizontalContainer(
                        listOf(
                            Renderable.verticalContainer(FarmingItems.getArmorDisplay(), 2),
                            Renderable.verticalContainer(FarmingItems.getEquipmentDisplay(), 2)
                        ),
                        2,
                        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER
                    ),
                    Renderable.horizontalContainer(FarmingItems.getPetsDisplay(true), 2)
                ),
                2,
                verticalAlign = RenderUtils.VerticalAlignment.BOTTOM
            ),
            144,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            verticalAlign = RenderUtils.VerticalAlignment.BOTTOM
        )
}
