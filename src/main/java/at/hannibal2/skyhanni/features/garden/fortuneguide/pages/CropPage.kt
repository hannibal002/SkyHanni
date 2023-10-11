package at.hannibal2.skyhanni.features.garden.fortuneguide.pages
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI.Companion.getItem
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFStats
import at.hannibal2.skyhanni.features.garden.fortuneguide.FarmingItems
import at.hannibal2.skyhanni.features.garden.fortuneguide.FortuneStats
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase

class CropPage: FFGuideGUI.FFGuidePage() {

    override fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float) {
        for (item in FarmingItems.entries) {
            if (item.name == FFGuideGUI.currentCrop?.name) {
                GuiRenderUtils.renderItemAndTip(item.getItem(), FFGuideGUI.guiLeft + 172, FFGuideGUI.guiTop + 60, mouseX, mouseY)
            }
        }

        var x: Int
        var y = FFGuideGUI.guiTop - 20
        var i = 0
        FFStats.cropPage.forEach { (key, value) ->
            if (key == FortuneStats.CROP_TOTAL) {
                val newLine = key.label.replace("Crop", FFGuideGUI.currentCrop?.name?.replace("_", " ")?.firstLetterUppercase()!!)
                GuiRenderUtils.drawFarmingBar(newLine, key.tooltip, value.first, value.second, FFGuideGUI.guiLeft + 135,
                    FFGuideGUI.guiTop + 5, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)
            } else {
                if (i % 2 == 0) {
                    x = FFGuideGUI.guiLeft + 15
                    y += 25
                } else {
                    x = FFGuideGUI.guiLeft + 255
                }
                i ++
                GuiRenderUtils.drawFarmingBar(key.label, key.tooltip, value.first, value.second, x, y,
                    90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)
            }
        }
    }
}