package at.hannibal2.skyhanni.features.garden.fortuneguide.pages

import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.TimeUtils

class OverviewPage: FFGuideGUI.FFGuidePage() {
    private val del = "TODO"

    override fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val cakeActive = FFGuideGUI.cakeBuffTime - System.currentTimeMillis() > 0 || FFGuideGUI.cakeBuffTime == -1L
        val timeUntilCakes = TimeUtils.formatDuration(FFGuideGUI.cakeBuffTime - System.currentTimeMillis())

        if (FFGuideGUI.breakdownMode) {
            RenderUtils.drawFarmingBar("§6Universal Farming Fortune", "§7§2Farming fortune in that is\n" +
                    "§2applied to every crop", 0, 1270, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 5, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            var line = if (FFGuideGUI.anitaBuff == -1) "§cAnita buff not saved\n§eVisit Anita to set it!"
            else "§7§2Fortune for levelling your Anita extra crops\n§2You get 2☘ per buff level\n§2Your upgrade level: ${FFGuideGUI.anitaBuff}"
            RenderUtils.drawFarmingBar("§2Anita Buff", line, FFGuideGUI.anitaBuff * 2, 30, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 30, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            line = if (FFGuideGUI.farmingLevel == -1) "§cFarming level not saved\n§eOpen /skills to set it!"
            else "§7§2Fortune for levelling your farming skill\n§2You get 4☘ per farming level\n§2Your farming level: ${FFGuideGUI.farmingLevel}"
            RenderUtils.drawFarmingBar("§2Farming Level", line, FFGuideGUI.farmingLevel * 4, 240, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 55, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            line = if (FFGuideGUI.communityUpgradeLevel == -1) "§cCommunity upgrade level not saved\n§eVisit Elizabeth to set it!"
            else "§7§2Fortune for community shop upgrades\n§2You get 4☘ per upgrade tier\n§2Your community upgrade level: ${FFGuideGUI.communityUpgradeLevel}"
            RenderUtils.drawFarmingBar("§2Community upgrades", line, FFGuideGUI.communityUpgradeLevel * 4, 40, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 80, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            line = if (FFGuideGUI.plotsUnlocked == -1) "§cUnlocked plot count not saved\n§eOpen /desk and view your plots to set it!"
            else "§7§2Fortune for unlocking garden plots\n§2You get 3☘ per plot unlocked\n§2Plots unlocked: ${FFGuideGUI.plotsUnlocked}"
            RenderUtils.drawFarmingBar("§2Garden Plots", line, FFGuideGUI.plotsUnlocked * 3, 72, FFGuideGUI.guiLeft + 15,
                FFGuideGUI.guiTop + 105, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            line = when (FFGuideGUI.cakeBuffTime) {
                -1L -> "§eYou have not eaten a cake since\n§egetting this feature, assuming you have\n§ethe buff active!"
                else -> "§7§2Fortune for eating cake\n§2You get 5☘ for eating cake\n§2Time until cake buff runs out: $timeUntilCakes"
            }
            if (FFGuideGUI.cakeBuffTime - System.currentTimeMillis() < 0) {
                line = "§cYour cake buff has run out\nGo eat some cake!"
            }
            RenderUtils.drawFarmingBar("§2Cake Buff", line, if (cakeActive) 5 else 0, 5, FFGuideGUI.guiLeft + 15,
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


            line = if (FFGuideGUI.currentEquipment == 0) "§7§2Total fortune from all your equipment\n§2Select a piece for more info"
            else "§7§2Total fortune from your\n${FFGuideGUI.items[13 + FFGuideGUI.currentEquipment].displayName}"
            RenderUtils.drawFarmingBar("§2Total Equipment Fortune", line, 0, if (FFGuideGUI.currentEquipment == 0) 198 else 49.5,
                FFGuideGUI.guiLeft + 255, FFGuideGUI.guiTop + 30, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            line = if (FFGuideGUI.currentEquipment == 0) "§7§2The base fortune from all your equipment\n§2Select a piece for more info"
            else "§7§2Total base fortune from your\n${FFGuideGUI.items[13 + FFGuideGUI.currentEquipment].displayName}"
            RenderUtils.drawFarmingBar("§2Equipment Base Fortune", line, 0, if (FFGuideGUI.currentEquipment == 0) 20 else 5,
                FFGuideGUI.guiLeft + 255, FFGuideGUI.guiTop + 55, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            line = if (FFGuideGUI.currentEquipment == 0) "§7§2The fortune from all of your equipment's abilities\n§2Select a piece for more info"
            else "§7§2Total ability fortune from your\n${FFGuideGUI.items[13 + FFGuideGUI.currentEquipment].displayName}"
            RenderUtils.drawFarmingBar("§2Equipment Ability", line, 0, if (FFGuideGUI.currentEquipment == 0) 60 else 15,
                FFGuideGUI.guiLeft + 255, FFGuideGUI.guiTop + 80, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            line = if (FFGuideGUI.currentEquipment == 0) "§7§2The fortune from all of your equipment's reforges\n§2Select a piece for more info"
            else "§7§2Total reforge fortune from your\n${FFGuideGUI.items[13 + FFGuideGUI.currentEquipment].displayName}"
            RenderUtils.drawFarmingBar("§2Equipment Reforge", line, 0, if (FFGuideGUI.currentEquipment == 0) 40 else 10,
                FFGuideGUI.guiLeft + 255, FFGuideGUI.guiTop + 105, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

            line = if (FFGuideGUI.currentEquipment == 0) "§7§2The fortune from all of your equipment's enchantments\n§2Select a piece for more info"
            else "§7§2Total enchantment fortune from your\n${FFGuideGUI.items[13 + FFGuideGUI.currentEquipment].displayName}"
            RenderUtils.drawFarmingBar("§2Equipment Enchantment", line, 0, if (FFGuideGUI.currentEquipment == 0) 78 else 19.5,
                FFGuideGUI.guiLeft + 255, FFGuideGUI.guiTop + 130, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)
        }
    }
}