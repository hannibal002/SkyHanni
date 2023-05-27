package at.hannibal2.skyhanni.features.garden.fortuneguide.pages

import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI.Companion.getItem
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFStats
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFTypes
import at.hannibal2.skyhanni.features.garden.fortuneguide.FarmingItems
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.RenderUtils


class CactusPage: FFGuideGUI.FFGuidePage() {
    override fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (FFGuideGUI.breakdownMode) {
            RenderUtils.renderItemAndTip(FarmingItems.CACTUS.getItem(),
                FFGuideGUI.guiLeft + 172, FFGuideGUI.guiTop + 60, mouseX, mouseY)

            val totalCropFF = FFStats.totalBaseFF[FFTypes.TOTAL]!! + FFStats.cactusFF[FFTypes.TOTAL]!!
            RenderUtils.drawFarmingBar("§6Cactus Farming Fortune", "§7§2Farming fortune for cacti",
                totalCropFF, 1548, FFGuideGUI.guiLeft + 135,
                FFGuideGUI.guiTop + 5, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Universal Farming Fortune", "§7§2Farming fortune in that is\n" +
                    "§2applied to every crop", FFStats.totalBaseFF[FFTypes.TOTAL] ?: 0, 1250, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 5, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Talisman Bonus", "§7§2Fortune from your talisman\n" +
                    "§2You get 10☘ per talisman tier", FFStats.cactusFF[FFTypes.ACCESSORY] ?: 0, 30, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 30, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Crop Upgrade", "§7§2Fortune from Desk crop upgrades\n" +
                    "§2You get 5☘ per level", FFStats.cactusFF[FFTypes.CROP_UPGRADE] ?: 0, 45, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 55, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Tool reforge", "§7§2Fortune from reforging your tool",
                FFStats.cactusFF[FFTypes.REFORGE] ?: 0, 16, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 80, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Farming for Dummies", "§7§2Fortune for each applied book\n" +
                    "§2You get 1☘ per applied book", FFStats.cactusFF[FFTypes.FFD] ?: 0, 5, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 105, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Harvesting Enchantment", "§7§2Fortune for each enchantment level\n" +
                    "§2You get 12.5☘ per level", FFStats.cactusFF[FFTypes.HARVESTING] ?: 0, 75, FFGuideGUI.guiLeft + 255,
                FFGuideGUI.guiTop + 5, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Cultivating Enchantment", "§7§2Fortune for each enchantment level\n" +
                    "§2You get 1☘ per level", FFStats.cactusFF[FFTypes.CULTIVATING] ?: 0, 10, FFGuideGUI.guiLeft + 255,
                FFGuideGUI.guiTop + 30, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Turbo-Cacti Enchantment", "§7§2Fortune for each enchantment level\n" +
                    "§2You get 5☘ per level", FFStats.cactusFF[FFTypes.TURBO] ?: 0, 25, FFGuideGUI.guiLeft + 255,
                FFGuideGUI.guiTop + 55, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Dedication Enchantment", "§7§2Fortune for each enchantment level\n" +
                    "§2and crop milestone", FFStats.cactusFF[FFTypes.DEDICATION] ?: 0, 92, FFGuideGUI.guiLeft + 255,
                FFGuideGUI.guiTop + 80, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)
        } else {
            return
        }
    }
}