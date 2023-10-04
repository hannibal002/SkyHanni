package at.hannibal2.skyhanni.features.garden.fortuneguide.pages

import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI.Companion.currentArmor
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI.Companion.currentEquipment
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI.Companion.getItem
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFStats
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFTypes
import at.hannibal2.skyhanni.features.garden.fortuneguide.FarmingItems
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.TimeUtils

class OverviewPage: FFGuideGUI.FFGuidePage() {
    private var equipmentFF = mutableMapOf<FFTypes, Double>()
    private var armorFF = mutableMapOf<FFTypes, Double>()

    override fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val timeUntilCakes = TimeUtils.formatDuration(FFStats.cakeExpireTime - System.currentTimeMillis())

        GuiRenderUtils.drawFarmingBar("§6Universal Farming Fortune",
            "§7§2Farming fortune in that is\n§2applied to every crop\n§eNot the same as tab FF\n" +
                    "§eSee on the grass block page", FFStats.totalBaseFF[FFTypes.TOTAL] ?: 0, 1277,
            FFGuideGUI.guiLeft + 15, FFGuideGUI.guiTop + 5, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

        var line = if (FFStats.baseFF[FFTypes.ANITA]!! < 0.0) "§cAnita buff not saved\n§eVisit Anita to set it!"
        else "§7§2Fortune for levelling your Anita extra crops\n§2You get 4☘ per buff level"
        GuiRenderUtils.drawFarmingBar("§2Anita Buff", line, FFStats.baseFF[FFTypes.ANITA] ?: 0.0, 60, FFGuideGUI.guiLeft + 15,
            FFGuideGUI.guiTop + 30, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

        line = if (FFStats.baseFF[FFTypes.FARMING_LVL]!! < 0.0) "§cFarming level not saved\n§eOpen /skills to set it!"
        else "§7§2Fortune for levelling your farming skill\n§2You get 4☘ per farming level"
        GuiRenderUtils.drawFarmingBar("§2Farming Level", line, FFStats.baseFF[FFTypes.FARMING_LVL] ?: 0.0, 240, FFGuideGUI.guiLeft + 15,
            FFGuideGUI.guiTop + 55, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

        line = if (FFStats.baseFF[FFTypes.COMMUNITY_SHOP]!! < 0.0) "§cCommunity upgrade level not saved\n§eVisit Elizabeth to set it!"
        else "§7§2Fortune for community shop upgrades\n§2You get 4☘ per upgrade tier"
        GuiRenderUtils.drawFarmingBar("§2Community upgrades", line, FFStats.baseFF[FFTypes.COMMUNITY_SHOP] ?: 0.0,
            40, FFGuideGUI.guiLeft + 15, FFGuideGUI.guiTop + 80, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

        line = if (FFStats.baseFF[FFTypes.PLOTS]!! < 0.0) "§cUnlocked plot count not saved\n§eOpen /desk and view your plots to set it!"
        else "§7§2Fortune for unlocking garden plots\n§2You get 3☘ per plot unlocked"
        GuiRenderUtils.drawFarmingBar("§2Garden Plots", line, FFStats.baseFF[FFTypes.PLOTS] ?: 0.0, 72, FFGuideGUI.guiLeft + 15,
            FFGuideGUI.guiTop + 105, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

        line = when (FFStats.cakeExpireTime) {
            -1L -> "§eYou have not eaten a cake since\n§edownloading this update, assuming the\n§ebuff is active!"
            else -> "§7§2Fortune for eating cake\n§2You get 5☘ for eating cake\n§2Time until cake buff runs out: $timeUntilCakes"
        }
        if (FFStats.cakeExpireTime - System.currentTimeMillis() < 0 && FFStats.cakeExpireTime != -1L) {
            line = "§cYour cake buff has run out\nGo eat some cake!"
        }
        GuiRenderUtils.drawFarmingBar("§2Cake Buff", line, FFStats.baseFF[FFTypes.CAKE] ?: 0.0, 5, FFGuideGUI.guiLeft + 15,
            FFGuideGUI.guiTop + 130, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

        val armorItem = when (currentArmor) {
            1 -> FarmingItems.HELMET
            2 -> FarmingItems.CHESTPLATE
            3 -> FarmingItems.LEGGINGS
            else -> FarmingItems.BOOTS
        }

        armorFF = when (currentArmor) {
            1 -> FFStats.helmetFF
            2 -> FFStats.chestplateFF
            3 -> FFStats.leggingsFF
            4 -> FFStats.bootsFF
            else -> FFStats.armorTotalFF
        }

        var word = if (currentArmor == 0) "Armor" else "Piece"

        line = if (currentArmor == 0) "§7§2Total fortune from your armor\n§2Select a piece for more info"
        else "§7§2Total fortune from your\n${armorItem.getItem().displayName}"
        var value = if (currentArmor == 0) {
            325
        } else if (FFStats.usingSpeedBoots) {
            when (currentArmor) {
                1 -> 76.67
                2, 3 -> 81.67
                else -> 85
            }
        } else {
            when (currentArmor) {
                1 -> 78.75
                2, 3 -> 83.75
                else -> 78.75
            }
        }
        GuiRenderUtils.drawFarmingBar("§2Total $word Fortune", line, armorFF[FFTypes.TOTAL] ?: 0, value,
            FFGuideGUI.guiLeft + 135, FFGuideGUI.guiTop + 30, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

        line = if (currentArmor == 0) "§7§2The base fortune from your armor\n§2Select a piece for more info"
        else "§7§2Base fortune from your\n${armorItem.getItem().displayName}"
        value = when (currentArmor) {
            0 -> {
                if (FFStats.usingSpeedBoots) 160 else 130
            }
            1 -> 30
            2 -> 35
            3 -> 35
            else -> {
                if (FFStats.usingSpeedBoots) 60 else 30
            }
        }
        GuiRenderUtils.drawFarmingBar("§2Base $word Fortune", line, armorFF[FFTypes.BASE] ?: 0,
            value, FFGuideGUI.guiLeft + 135,
            FFGuideGUI.guiTop + 55, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

        line = if (currentArmor == 0) "§7§2The fortune from your armor's ability\n§2Select a piece for more info"
        else "§7§2Ability fortune from your\n${armorItem.getItem().displayName}"
        value = if (FFStats.usingSpeedBoots) {
            when (currentArmor) {
                0 -> 50
                4 -> 0
                else -> 16.667
            }
        } else {
            when (currentArmor) {
                0 -> 75
                else -> 18.75
            }
        }

        GuiRenderUtils.drawFarmingBar("§2$word Ability", line, armorFF[FFTypes.ABILITY] ?: 0,
            value, FFGuideGUI.guiLeft + 135,
            FFGuideGUI.guiTop + 80, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

        line = if (currentArmor == 0) "§7§2The fortune from your armor's reforge\n§2Select a piece for more info"
        else "§7§2Total fortune from your\n${armorItem.getItem().displayName}"
        value = if (currentArmor == 0) {
            if (FFStats.usingSpeedBoots) 115 else 120
        } else if (currentArmor == 4) {
            if (FFStats.usingSpeedBoots) 25 else 30
        } else 30
        GuiRenderUtils.drawFarmingBar("§2$word Reforge", line, armorFF[FFTypes.REFORGE] ?: 0,
            value, FFGuideGUI.guiLeft + 135,
            FFGuideGUI.guiTop + 105, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

        var currentPet = FFStats.rabbitFF
        var petMaxFF = 60
        when (FFGuideGUI.currentPet) {
            FarmingItems.ELEPHANT -> {
                currentPet = FFStats.elephantFF
                petMaxFF = 210
            }
            FarmingItems.MOOSHROOM_COW -> {
                currentPet = FFStats.mooshroomFF
                petMaxFF = 217
            }
            FarmingItems.BEE -> {
                currentPet = FFStats.beeFF
                petMaxFF = 90
            }
            else -> {}
        }

        GuiRenderUtils.drawFarmingBar("§2Total Pet Fortune", "§7§2The total fortune from your pet and its item",
            currentPet[FFTypes.TOTAL] ?: 0, petMaxFF, FFGuideGUI.guiLeft + 105,
            FFGuideGUI.guiTop + 155, 70, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

        line = when (FFStats.currentPetItem) {
            "GREEN_BANDANA" -> "§7§2The fortune from your pet's item\n§2Grants 4☘ per garden level"
            "YELLOW_BANDANA" -> "§7§2The fortune from your pet's item"
            "MINOS_RELIC" -> "§cGreen Bandana is better for fortune than minos relic!"
            else -> "No fortune boosting pet item"
        }
        GuiRenderUtils.drawFarmingBar("§2Pet Item", line, currentPet[FFTypes.PET_ITEM] ?: 0, 60, FFGuideGUI.guiLeft + 185,
            FFGuideGUI.guiTop + 155, 70, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

        word = if (currentEquipment == 0) "Equipment" else "Piece"

        val equipmentItem = when (currentEquipment) {
            1 -> FarmingItems.NECKLACE
            2 -> FarmingItems.CLOAK
            3 -> FarmingItems.BELT
            else -> FarmingItems.BRACELET
        }

        equipmentFF = when (currentEquipment) {
            1 -> FFStats.necklaceFF
            2 -> FFStats.cloakFF
            3 -> FFStats.beltFF
            4 -> FFStats.braceletFF
            else -> FFStats.equipmentTotalFF
        }

        line = if (currentEquipment == 0) "§7§2Total fortune from all your equipment\n§2Select a piece for more info"
        else "§7§2Total fortune from your\n${equipmentItem.getItem().displayName}"
        GuiRenderUtils.drawFarmingBar("§2Total $word Fortune", line, equipmentFF[FFTypes.TOTAL] ?: 0,
            if (currentEquipment == 0) 218 else 54.5,
            FFGuideGUI.guiLeft + 255, FFGuideGUI.guiTop + 30, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

        line = if (currentEquipment == 0) "§7§2The base fortune from all your equipment\n§2Select a piece for more info"
        else "§7§2Total base fortune from your\n${equipmentItem.getItem().displayName}"
        GuiRenderUtils.drawFarmingBar("§2$word Base Fortune", line, equipmentFF[FFTypes.BASE] ?: 0,
            if (currentEquipment == 0) 20 else 5,
            FFGuideGUI.guiLeft + 255, FFGuideGUI.guiTop + 55, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

        line = if (currentEquipment == 0) "§7§2The fortune from all of your equipment's abilities\n§2Select a piece for more info"
        else "§7§2Total ability fortune from your\n${equipmentItem.getItem().displayName}"
        GuiRenderUtils.drawFarmingBar("§2$word Ability", line, equipmentFF[FFTypes.ABILITY] ?: 0,
            if (currentEquipment == 0) 60 else 15,
            FFGuideGUI.guiLeft + 255, FFGuideGUI.guiTop + 80, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

        line = if (currentEquipment == 0) "§7§2The fortune from all of your equipment's reforges\n§2Select a piece for more info"
        else "§7§2Total reforge fortune from your\n${equipmentItem.getItem().displayName}"
        GuiRenderUtils.drawFarmingBar("§2$word Reforge", line, equipmentFF[FFTypes.REFORGE] ?: 0,
            if (currentEquipment == 0) 60 else 15,
            FFGuideGUI.guiLeft + 255, FFGuideGUI.guiTop + 105, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)

        line = if (currentEquipment == 0) "§7§2The fortune from all of your equipment's enchantments\n§2Select a piece for more info"
        else "§7§2Total enchantment fortune from your\n${equipmentItem.getItem().displayName}"
        GuiRenderUtils.drawFarmingBar("§2$word Enchantment", line, equipmentFF[FFTypes.GREEN_THUMB] ?: 0,
            if (currentEquipment == 0) 78 else 19.5,
            FFGuideGUI.guiLeft + 255, FFGuideGUI.guiTop + 130, 90, mouseX, mouseY, FFGuideGUI.tooltipToDisplay)
    }
}