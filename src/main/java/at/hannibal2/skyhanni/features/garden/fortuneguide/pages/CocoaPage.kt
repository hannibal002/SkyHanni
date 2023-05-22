package at.hannibal2.skyhanni.features.garden.fortuneguide.pages

import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.utils.RenderUtils


class CocoaPage: FFGuideGUI.FFGuidePage() {
//    private val tool = GardenAPI.config?.fortune?.farmingItems?.get(9)
    private val textLinesWithTooltip = mutableMapOf<Pair<String, String>, Pair<Int, Int>>()
    private val del = "TODO"

    override fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (FFGuideGUI.breakdownMode) {
//            RenderUtils.renderItemAndTip(tool, FFGuideGUI.guiLeft + 172, FFGuideGUI.guiTop + 60, mouseX, mouseY)
            textLinesWithTooltip[Pair("§6Cocoa Bean Farming Fortune", "")] = Pair(FFGuideGUI.guiLeft + 140,  FFGuideGUI.guiTop + 5)
            textLinesWithTooltip[Pair("§2$del / 1575.5☘   $del%", "")] = Pair(FFGuideGUI.guiLeft + 140,  FFGuideGUI.guiTop + 15)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 145, FFGuideGUI.guiTop + 30, 80, 0f)

            textLinesWithTooltip[Pair("§2Base Farming Fortune", "")] = Pair(FFGuideGUI.guiLeft + 15,  FFGuideGUI.guiTop)
            textLinesWithTooltip[Pair("§2$del / 1270☘      $del%", "")] = Pair(FFGuideGUI.guiLeft + 15,  FFGuideGUI.guiTop + 10)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 20, FFGuideGUI.guiTop + 20, 90, 0f)

            textLinesWithTooltip[Pair("§2Talisman Bonus", "§7§2Fortune from your talisman\n" +
                    "§2You get 10☘ per talisman tier\n§2Your unlocked tier: $del")] = Pair(FFGuideGUI.guiLeft + 15,  FFGuideGUI.guiTop + 30)
            textLinesWithTooltip[Pair("§2$del / 30☘      $del%", "")] = Pair(FFGuideGUI.guiLeft + 15,  FFGuideGUI.guiTop + 40)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 20, FFGuideGUI.guiTop + 50, 90, 0f)

            textLinesWithTooltip[Pair("§2Crop Upgrade", "§7§2Fortune from Desk crop upgrades\n" +
                    "§2You get 5☘ per level\n§2Your unlocked level: $del")] = Pair(FFGuideGUI.guiLeft + 15,  FFGuideGUI.guiTop + 60)
            textLinesWithTooltip[Pair("§2$del / 45☘      $del%", "")] = Pair(FFGuideGUI.guiLeft + 15,  FFGuideGUI.guiTop + 70)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 20, FFGuideGUI.guiTop + 80, 90, 0f)

            textLinesWithTooltip[Pair("§2Tool reforge", "§7§2Fortune from reforging your tool")] = Pair(FFGuideGUI.guiLeft + 15,  FFGuideGUI.guiTop + 90)
            textLinesWithTooltip[Pair("§2$del / 16☘   $del%", "")] = Pair(FFGuideGUI.guiLeft + 15,  FFGuideGUI.guiTop + 100)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 20, FFGuideGUI.guiTop + 110, 90, 0f)

            textLinesWithTooltip[Pair("§2Farming for Dummies", "§7§2Fortune for applied farming for dummies books\n" +
                    "§2You get 1☘ per applied book\n§2Your applied books: $del")] = Pair(FFGuideGUI.guiLeft + 15,  FFGuideGUI.guiTop + 120)
            textLinesWithTooltip[Pair("§2$del / 5☘    $del%", "")] = Pair(FFGuideGUI.guiLeft + 15,  FFGuideGUI.guiTop + 130)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 20, FFGuideGUI.guiTop + 140, 90, 0f)

            textLinesWithTooltip[Pair("§2Tool bonus", "§7§2Bonus 20☘ for using the tool")] = Pair(FFGuideGUI.guiLeft + 245,  FFGuideGUI.guiTop)
            textLinesWithTooltip[Pair("§2$del / 20☘      $del%", "")] = Pair(FFGuideGUI.guiLeft + 245,  FFGuideGUI.guiTop + 10)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 245, FFGuideGUI.guiTop + 20, 90, 1f)

            textLinesWithTooltip[Pair("§2Sunder Enchantment", "§7§2Fortune for each enchantment level\n" +
                    "§2You get 12.5☘ per level\n§2Your enchantment level: $del")] = Pair(FFGuideGUI.guiLeft + 245,  FFGuideGUI.guiTop + 30)
            textLinesWithTooltip[Pair("§2$del / 62.5☘      $del%", "")] = Pair(FFGuideGUI.guiLeft + 245,  FFGuideGUI.guiTop + 40)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 245, FFGuideGUI.guiTop + 50, 90, 0f)

            textLinesWithTooltip[Pair("§2Cultivating Enchantment", "§7§2Fortune for each enchantment level\n" +
                    "§2You get 1☘ per level\n§2Your enchantment level: $del\n§2Farm crops to level up")] = Pair(FFGuideGUI.guiLeft + 245,  FFGuideGUI.guiTop + 60)
            textLinesWithTooltip[Pair("§2$del / 10☘      $del%", "")] = Pair(FFGuideGUI.guiLeft + 245,  FFGuideGUI.guiTop + 70)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 245, FFGuideGUI.guiTop + 80, 90, 0f)

            textLinesWithTooltip[Pair("§2Turbo-Cocoa Enchantment", "§7§2Fortune for each enchantment level\n" +
                    "§2You get 5☘ per level\n§2Your enchantment level: $del")] = Pair(FFGuideGUI.guiLeft + 245,  FFGuideGUI.guiTop + 90)
            textLinesWithTooltip[Pair("§2$del / 25☘      $del%", "")] = Pair(FFGuideGUI.guiLeft + 245,  FFGuideGUI.guiTop + 100)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 245, FFGuideGUI.guiTop + 110, 90, 0f)

            textLinesWithTooltip[Pair("§2Dedication Enchantment", "§7§2Fortune for each enchantment level\n" +
                    "§2You get $del☘ per level\n§2Your enchantment level: $del")] = Pair(FFGuideGUI.guiLeft + 245,  FFGuideGUI.guiTop + 120)
            textLinesWithTooltip[Pair("§2$del / 92☘      $del%", "")] = Pair(FFGuideGUI.guiLeft + 245,  FFGuideGUI.guiTop + 130)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 245, FFGuideGUI.guiTop + 140, 90, 0f)
        } else {

        }
    }
}