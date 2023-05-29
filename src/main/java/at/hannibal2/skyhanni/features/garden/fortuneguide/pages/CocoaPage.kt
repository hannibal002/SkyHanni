package at.hannibal2.skyhanni.features.garden.fortuneguide.pages

import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI.Companion.getItem
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFStats
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFTypes
import at.hannibal2.skyhanni.features.garden.fortuneguide.FarmingItems
import at.hannibal2.skyhanni.utils.GuiRenderUtils

class CocoaPage: FFGuideGUI.FFGuidePage() {
    override fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (FFGuideGUI.breakdownMode) {
            GuiRenderUtils.renderItemAndTip(FarmingItems.COCOA_BEANS.getItem(),
                FFGuideGUI.guiLeft + 172, FFGuideGUI.guiTop + 60, mouseX, mouseY)

            val totalCropFF = FFStats.totalBaseFF[FFTypes.TOTAL]!! + FFStats.cocoaFF[FFTypes.TOTAL]!!
            GuiRenderUtils.drawFarmingBar("§6Cocoa Farming Fortune", "§7§2Farming fortune for cocoa beans",
                totalCropFF, 1555.5, FFGuideGUI.guiLeft + 135,
                FFGuideGUI.guiTop + 5, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            GuiRenderUtils.drawFarmingBar("§2Universal Farming Fortune", "§7§2Farming fortune in that is\n" +
                    "§2applied to every crop", FFStats.totalBaseFF[FFTypes.TOTAL] ?: 0, 1250, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 5, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            GuiRenderUtils.drawFarmingBar("§2Talisman Bonus", "§7§2Fortune from your talisman\n" +
                    "§2You get 10☘ per talisman tier", FFStats.cocoaFF[FFTypes.ACCESSORY] ?: 0, 30, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 30, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            GuiRenderUtils.drawFarmingBar("§2Crop Upgrade", "§7§2Fortune from Desk crop upgrades\n" +
                    "§2You get 5☘ per level", FFStats.cocoaFF[FFTypes.CROP_UPGRADE] ?: 0, 45, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 55, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            GuiRenderUtils.drawFarmingBar("§2Tool reforge", "§7§2Fortune from reforging your tool",
                FFStats.cocoaFF[FFTypes.REFORGE] ?: 0, 16, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 80, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            GuiRenderUtils.drawFarmingBar("§2Farming for Dummies", "§7§2Fortune for each applied book\n" +
                    "§2You get 1☘ per applied book", FFStats.cocoaFF[FFTypes.FFD] ?: 0, 5, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 105, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            GuiRenderUtils.drawFarmingBar("§2Base tool fortune", "§7§2You get 20☘ for using the cocoa chopper",
                FFStats.cocoaFF[FFTypes.BASE] ?: 0, 20, FFGuideGUI.guiLeft + 255,
                FFGuideGUI.guiTop + 5, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            GuiRenderUtils.drawFarmingBar("§2Sunder Enchantment", "§7§2Fortune for each enchantment level\n" +
                    "§2You get 12.5☘ per level", FFStats.cocoaFF[FFTypes.SUNDER] ?: 0, 62.5, FFGuideGUI.guiLeft + 255,
                FFGuideGUI.guiTop + 30, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            GuiRenderUtils.drawFarmingBar("§2Cultivating Enchantment", "§7§2Fortune for each enchantment level\n" +
                    "§2You get 1☘ per level", FFStats.cocoaFF[FFTypes.CULTIVATING] ?: 0, 10, FFGuideGUI.guiLeft + 255,
                FFGuideGUI.guiTop + 55, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            GuiRenderUtils.drawFarmingBar("§2Turbo-Cocoa Enchantment", "§7§2Fortune for each enchantment level\n" +
                    "§2You get 5☘ per level", FFStats.cocoaFF[FFTypes.TURBO] ?: 0, 25, FFGuideGUI.guiLeft + 255,
                FFGuideGUI.guiTop + 80, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            GuiRenderUtils.drawFarmingBar("§2Dedication Enchantment", "§7§2Fortune for each enchantment level\n" +
                    "§2and crop milestone", FFStats.cocoaFF[FFTypes.DEDICATION] ?: 0, 92, FFGuideGUI.guiLeft + 255,
                FFGuideGUI.guiTop + 105, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)
        } else {
            return
        }
    }
}