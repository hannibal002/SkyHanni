package at.hannibal2.skyhanni.features.garden.fortuneguide.pages

import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.utils.RenderUtils
import net.minecraft.client.gui.GuiScreen

class OverviewPage: FFGuideGUI.FFGuidePage() {
    private val textLinesWithTooltip = mutableMapOf<Pair<String, String>, Pair<Int, Int>>()
    private val del = "TODO"

    override fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float) {
        //TODO fix up how this looks
        if (FFGuideGUI.breakdownMode && FFGuideGUI.currentMode == 0) {
            textLinesWithTooltip[Pair("§6Universal Farming Fortune", "§7§2Farming fortune in general\n" +
                    "§2Most of this shows in the tablist")] = Pair(FFGuideGUI.guiLeft + 140,  FFGuideGUI.guiTop + 5)
            textLinesWithTooltip[Pair("§2$del / 1270☘   $del%", "")] = Pair(FFGuideGUI.guiLeft + 140,  FFGuideGUI.guiTop + 15)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 145, FFGuideGUI.guiTop + 30, 80, 0f)

            textLinesWithTooltip[Pair("§2Base Farming Fortune", "")] = Pair(FFGuideGUI.guiLeft + 15,  FFGuideGUI.guiTop + 5)
            textLinesWithTooltip[Pair("§2100☘   $del%", "")] = Pair(FFGuideGUI.guiLeft + 15,  FFGuideGUI.guiTop + 15)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 20, FFGuideGUI.guiTop + 25, 90, 1f)

            textLinesWithTooltip[Pair("§2Farming Level", "§7§2Fortune for levelling your farming skill\n" +
                    "§2You get 4☘ per farming level\n§2Your farming level: $del")] = Pair(FFGuideGUI.guiLeft + 255,  FFGuideGUI.guiTop + 5)
            textLinesWithTooltip[Pair("§2$del / 240☘      $del%", "")] = Pair(FFGuideGUI.guiLeft + 255,  FFGuideGUI.guiTop + 15)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 255, FFGuideGUI.guiTop + 25, 90, 0f)

            textLinesWithTooltip[Pair("§2Community upgrades", "§7§2Fortune for community shop upgrades\n" +
                    "§2You get 4☘ per talisman tier\n§2Your community upgrade level: $del")] = Pair(FFGuideGUI.guiLeft + 15,  FFGuideGUI.guiTop + 35)
            textLinesWithTooltip[Pair("§2$del / 40☘      $del%", "")] = Pair(FFGuideGUI.guiLeft + 15,  FFGuideGUI.guiTop + 45)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 20, FFGuideGUI.guiTop + 55, 90, 0f)

            textLinesWithTooltip[Pair("§2Garden Plots", "§7§2Fortune for unlocking garden plots\n" +
                    "§2You get 3☘ per plot unlocked\n§2Plots unlocked: $del")] = Pair(FFGuideGUI.guiLeft + 255,  FFGuideGUI.guiTop + 35)
            textLinesWithTooltip[Pair("§2$del / 72☘     $del%", "")] = Pair(FFGuideGUI.guiLeft + 255,  FFGuideGUI.guiTop + 45)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 255, FFGuideGUI.guiTop + 55, 90, 0f)

            textLinesWithTooltip[Pair("§2Cake Buff", "§7§2Fortune for eating cake\n" +
                    "§2You get 5☘ for eating cake\n§2Time until cake runs out: $del")] = Pair(FFGuideGUI.guiLeft + 145,  FFGuideGUI.guiTop + 35)
            textLinesWithTooltip[Pair("§2$del / 5☘     $del%", "")] = Pair(FFGuideGUI.guiLeft + 145,  FFGuideGUI.guiTop + 45)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 145, FFGuideGUI.guiTop + 55, 80, 0f)
            // prompt to select a pet if they haven't
            RenderUtils.drawStringCentered("§9§nPets", FFGuideGUI.guiLeft + 180, FFGuideGUI.guiTop + 75)

            textLinesWithTooltip[Pair("§2Total Pet Fortune", "§7§2Total fortune from your pet\n" +
                    "§2Your pet without an item grants $del☘")] = Pair(FFGuideGUI.guiLeft + 140,  FFGuideGUI.guiTop + 105)
            textLinesWithTooltip[Pair("§2$del / $del☘  $del%", "")] = Pair(FFGuideGUI.guiLeft + 140,  FFGuideGUI.guiTop + 115)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 140, FFGuideGUI.guiTop + 125, 80, 0f)

            textLinesWithTooltip[Pair("§2Pet Item", "§7§2Fortune from your pet's item\n" +
                    // $del% stat boost, 30☘  : Minos relic effect = $del☘   :  If they are using relic say its bad and to sell
                    "§2Grants 4☘ per garden level\n§2Your garden level: $del")] = Pair(FFGuideGUI.guiLeft + 140,  FFGuideGUI.guiTop + 135)
            textLinesWithTooltip[Pair("§2$del / 60☘     $del%", "")] = Pair(FFGuideGUI.guiLeft + 140,  FFGuideGUI.guiTop + 145)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 140, FFGuideGUI.guiTop + 155, 80, 0f)

            RenderUtils.drawStringCentered("§9§nArmor", FFGuideGUI.guiLeft + 60, FFGuideGUI.guiTop + 75)

            textLinesWithTooltip[Pair("§2Total Armor Fortune", "§7§2Total fortune from your armor\n" +
                    "§2Click for more info")] = Pair(FFGuideGUI.guiLeft + 15,  FFGuideGUI.guiTop + 105)
            textLinesWithTooltip[Pair("§2$del / $del☘   $del%", "")] = Pair(FFGuideGUI.guiLeft + 15,  FFGuideGUI.guiTop + 115)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 20, FFGuideGUI.guiTop + 125, 90, 0f)
            //todo these buttons don't fit
            GuiScreen.drawRect(FFGuideGUI.guiLeft + 15, FFGuideGUI.guiTop + 130, FFGuideGUI.guiLeft + 85,
                FFGuideGUI.guiTop + 150, 0x50000000)
            RenderUtils.drawStringCentered("§6More Armor Stats", FFGuideGUI.guiLeft + 60, FFGuideGUI.guiTop + 140)

            RenderUtils.drawStringCentered("§9§nEquipment", FFGuideGUI.guiLeft + 295, FFGuideGUI.guiTop + 75)

            textLinesWithTooltip[Pair("§2Total Equipment Fortune", "§7§2Total fortune from your equipment\n" +
                    "§2Click for more info")] = Pair(FFGuideGUI.guiLeft + 255,  FFGuideGUI.guiTop + 105)
            textLinesWithTooltip[Pair("§2$del / $del☘    $del%", "")] = Pair(FFGuideGUI.guiLeft + 255,  FFGuideGUI.guiTop + 115)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 255, FFGuideGUI.guiTop + 125, 90, 0f)

            GuiScreen.drawRect(FFGuideGUI.guiLeft + 255, FFGuideGUI.guiTop + 130, FFGuideGUI.guiLeft + 325,
                FFGuideGUI.guiTop + 150, 0x50000000)
            RenderUtils.drawStringCentered("§6More Equipment Stats", FFGuideGUI.guiLeft + 290, FFGuideGUI.guiTop + 140)
        } // not implemented yet
        else if (FFGuideGUI.breakdownMode && FFGuideGUI.currentMode == 1) {
            textLinesWithTooltip[Pair("§6Total Armor Fortune", "")] = Pair(FFGuideGUI.guiLeft + 140,  FFGuideGUI.guiTop + 5)
            textLinesWithTooltip[Pair("§2$del / $del☘   $del%", "")] = Pair(FFGuideGUI.guiLeft + 140,  FFGuideGUI.guiTop + 15)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 145, FFGuideGUI.guiTop + 30, 80, 0f)
        } else if (FFGuideGUI.breakdownMode && FFGuideGUI.currentMode == 2) {
            textLinesWithTooltip[Pair("§6Total Equipment Fortune", "")] = Pair(FFGuideGUI.guiLeft + 140,  FFGuideGUI.guiTop + 5)
            textLinesWithTooltip[Pair("§2$del / $del☘   $del%", "")] = Pair(FFGuideGUI.guiLeft + 140,  FFGuideGUI.guiTop + 15)
            RenderUtils.drawProgressBar(FFGuideGUI.guiLeft + 145, FFGuideGUI.guiTop + 30, 80, 0f)
        }

        FFGuideGUI.renderText(textLinesWithTooltip)
    }

    override fun swapMode() {
        textLinesWithTooltip.clear()
    }
}