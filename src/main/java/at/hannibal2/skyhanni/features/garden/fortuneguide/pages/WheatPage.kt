package at.hannibal2.skyhanni.features.garden.fortuneguide.pages

import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI.Companion.getItem
import at.hannibal2.skyhanni.features.garden.fortuneguide.FarmingItems
import at.hannibal2.skyhanni.utils.RenderUtils


class WheatPage: FFGuideGUI.FFGuidePage() {
    private val del = "TODO"

    override fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (FFGuideGUI.breakdownMode) {
            RenderUtils.renderItemAndTip(FarmingItems.WHEAT.getItem(),
                FFGuideGUI.guiLeft + 172, FFGuideGUI.guiTop + 60, mouseX, mouseY)


            RenderUtils.drawFarmingBar("§6Wheat Farming Fortune", "§7§2Farming fortune for wheat",
                0, 1766, FFGuideGUI.guiLeft + 135,
                FFGuideGUI.guiTop + 5, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Universal Farming Fortune", "§7§2Farming fortune in that is\n" +
                    "§2applied to every crop", 0, 1270, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 5, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Talisman Bonus", "§7§2Fortune from your talisman\n" +
                    "§2You get 10☘ per talisman tier\n§2Your unlocked tier: $del", 0, 30, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 30, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Crop Upgrade", "§7§2Fortune from Desk crop upgrades\n" +
                    "§2You get 5☘ per level\n§2Your unlocked level: $del", 0, 45, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 55, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Base tool fortune", "§7§2Depends on your tools tier\n" +
                    "§2You get 10☘/25☘/50☘\n§2Your tool tier: $del", 0, 50, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 80, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)
            // stuff about the reforge
            RenderUtils.drawFarmingBar("§2Tool reforge", "§7§2Fortune from reforging your tool",
                0, 20, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 105, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Farming for Dummies", "§7§2Fortune for each applied book\n" +
                    "§2You get 1☘ per applied book\n§2Your applied books: $del", 0, 5, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 130, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            //maybe display the amount of digits - 4 as well
            RenderUtils.drawFarmingBar("§2Logarithmic Counter", "§7§2Fortune from increasing crop counter\n" +
                    "§2You get 16☘ per digit - 4\n§2Your counter value: $del", 0, 96, FFGuideGUI.guiLeft + 255,
                FFGuideGUI.guiTop + 5, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Collection Analyst", "§7§2Fortune from increasing crop collection\n" +
                    "§2You get 8☘ per digit - 4\n§2Your collection value: $del", 0, 48, FFGuideGUI.guiLeft + 255,
                FFGuideGUI.guiTop + 30, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Harvesting Enchantment", "§7§2Fortune for each enchantment level\n" +
                    "§2You get 12.5☘ per level\n§2Your enchantment level: $del", 0, 75, FFGuideGUI.guiLeft + 255,
                FFGuideGUI.guiTop + 55, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Cultivating Enchantment", "§7§2Fortune for each enchantment level\n" +
                    "§2You get 1☘ per level\n§2Your enchantment level: $del\n§2Farm $del crops to level up", 0, 10, FFGuideGUI.guiLeft + 255,
                FFGuideGUI.guiTop + 80, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Turbo-Wheat Enchantment", "§7§2Fortune for each enchantment level\n" +
                    "§2You get 5☘ per level\n§2Your enchantment level: $del", 0, 25, FFGuideGUI.guiLeft + 255,
                FFGuideGUI.guiTop + 105, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Dedication Enchantment", "§7§2Fortune for each enchantment level\n" +
                    "§2You get $del☘ per level\n§2Your enchantment level: $del", 0, 92, FFGuideGUI.guiLeft + 255,
                FFGuideGUI.guiTop + 130, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)
        } else {

        }
    }
}