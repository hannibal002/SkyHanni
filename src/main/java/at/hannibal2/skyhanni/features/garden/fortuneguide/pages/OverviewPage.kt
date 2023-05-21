package at.hannibal2.skyhanni.features.garden.fortuneguide.pages

import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.utils.RenderUtils

class OverviewPage: FFGuideGUI.FFGuidePage() {
    private val del = "TODO"

    override fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float) {
        //todo calculate time until runs out, and a warning if it hasn't been discovered yet
        val cakeBuffActive = FFGuideGUI.cakeBuffTime - System.currentTimeMillis() > 0
        println(cakeBuffActive)
        if (FFGuideGUI.breakdownMode) {
            RenderUtils.drawFarmingBar("§6Universal Farming Fortune", "§7§2Farming fortune in that is\n" +
                    "§2applied to every crop", 0, 1270, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 5, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Anita Buff", "§7§Farming fortune from upgrading your\n" +
                    "§2drops at the Anita NPC", 0, 30, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 30, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Farming Level", "§7§2Fortune for levelling your farming skill\n" +
                    "§2You get 4☘ per farming level\n§2Your farming level: ${FFGuideGUI.farmingLevel}", FFGuideGUI.farmingLevel * 4, 240, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 55, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Community upgrades", "§7§2Fortune for community shop upgrades\n" +
                    "§2You get 4☘ per talisman tier\n§2Your community upgrade level: ${FFGuideGUI.communityUpgradeLevel}", FFGuideGUI.communityUpgradeLevel * 4, 40, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 80, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Garden Plots", "§7§2Fortune for unlocking garden plots\n" +
                    "§2You get 3☘ per plot unlocked\n§2Plots unlocked: ${FFGuideGUI.plotsUnlocked}", FFGuideGUI.plotsUnlocked * 3, 72, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 105, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Cake Buff", "§7§2Fortune for eating cake\n" +
                    "§2You get 5☘ for eating cake\n§2Time until cake runs out: $del", 0, 5, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 130, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)


            //If one piece is selected show that ones stats, otherwise a total
            RenderUtils.drawFarmingBar("§2Total Armor Fortune", "§7§2Total fortune from your armor\n" +
                    "§2Select a piece for more info", 0, 10000, FFGuideGUI.guiLeft + 135,
                FFGuideGUI.guiTop + 30, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)
            // ranchers boots bonus will be included as base fortune, and not an ability
            RenderUtils.drawFarmingBar("§2Base Armor Fortune", "§7§2The base fortune from your armor\n" +
                    "§2Select a piece for more info", 0, 10000, FFGuideGUI.guiLeft + 135,
                FFGuideGUI.guiTop + 55, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)
            //If one singular peice is selected, show the full ability
            RenderUtils.drawFarmingBar("§2Armor Ability", "§7§2The fortune from your armor's ability\n" +
                    "§2Select a piece for more info", 0, 10000, FFGuideGUI.guiLeft + 135,
                FFGuideGUI.guiTop + 80, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)
            // display what it could be at better reforges when a single piece is selected
            RenderUtils.drawFarmingBar("§2Armor Reforge", "§7§2The fortune from your armor's reforge\n" +
                    "§2Select a piece for more info", 0, 10000, FFGuideGUI.guiLeft + 135,
                FFGuideGUI.guiTop + 105, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)


            RenderUtils.drawFarmingBar("§2Total Pet Fortune", "§7§2The total fortune from your pet and its item",
                0, 10000, FFGuideGUI.guiLeft + 75,
                FFGuideGUI.guiTop + 155, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Pet Item", "§7§2The fortune from your pet's item\n" +
                    "§2Grants 4☘ per garden level\n§2Your garden level: $del", 0, 60, FFGuideGUI.guiLeft + 195,
                FFGuideGUI.guiTop + 155, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)


            RenderUtils.drawFarmingBar("§2Total Equipment Fortune", "§7§2Total fortune from your equipment\n" +
                    "§2Select a piece for more info", 0, 10000, FFGuideGUI.guiLeft + 255,
                FFGuideGUI.guiTop + 30, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Equipment Base Fortune", "§7§2The base fortune from your equipment\n" +
                    "§2Select a piece for more info", 0, 10000, FFGuideGUI.guiLeft + 255,
                FFGuideGUI.guiTop + 55, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Equipment Ability", "§7§2The fortune from your equipment's ability\n" +
                    "§2Select a piece for more info", 0, 10000, FFGuideGUI.guiLeft + 255,
                FFGuideGUI.guiTop + 80, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Equipment Reforge", "§7§2The fortune from your equipment's reforge\n" +
                    "§2Select a piece for more info", 0, 10000, FFGuideGUI.guiLeft + 255,
                FFGuideGUI.guiTop + 105, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            RenderUtils.drawFarmingBar("§2Equipment Enchantment", "§7§2The fortune from your equipment's enchantment\n" +
                    "§2Select a piece for more info", 0, 10000, FFGuideGUI.guiLeft + 255,
                FFGuideGUI.guiTop + 130, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)
        }
    }
}