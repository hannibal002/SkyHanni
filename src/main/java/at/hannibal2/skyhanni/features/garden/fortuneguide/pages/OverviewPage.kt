package at.hannibal2.skyhanni.features.garden.fortuneguide.pages

import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFStats
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFTypes
import at.hannibal2.skyhanni.features.garden.fortuneguide.FarmingItems
import at.hannibal2.skyhanni.utils.CollectionUtils.getOrNull
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.guide.GuideTablePage
import at.hannibal2.skyhanni.utils.renderables.Renderable

class OverviewPage(sizeX: Int, sizeY: Int, paddingX: Int = 15, paddingY: Int = 7, footerSpacing: Int = 6) :
    GuideTablePage(
        sizeX, sizeY, paddingX, paddingY, footerSpacing
    ) {

    private var equipmentFF = mapOf<FFTypes, Double>()
    private var armorFF = mapOf<FFTypes, Double>()

    override fun onEnter() {
        val (content, footer) = getPage()
        update(content, footer)
    }

    fun getPage(): Pair<List<List<Renderable>>, List<Renderable>> {
        val content = mutableListOf<MutableList<Renderable>>()
        val footer = mutableListOf<Renderable>()
        val timeUntilCakes = TimeUtils.formatDuration(FFStats.cakeExpireTime - System.currentTimeMillis())

        content.addTable(
            0,
            GuiRenderUtils.getFarmingBar(
                label = "§6Universal Farming Fortune",
                tooltip = "§7§2Farming fortune in that is\n§2applied to every crop\n§eNot the same as tab FF\n" +
                    "§eSee on the grass block page",
                currentValue = FFStats.totalBaseFF[FFTypes.TOTAL] ?: 0,
                maxValue = 1277,
                width = 90
            )
        )

        var line = if (FFTypes.ANITA.notSaved()) "§cAnita buff not saved\n§eVisit Anita to set it!"
        else "§7§2Fortune for levelling your Anita extra crops\n§2You get 4☘ per buff level"

        content.addTable(
            1,
            GuiRenderUtils.getFarmingBar(
                label = "§2Anita Buff",
                tooltip = line,
                currentValue = FFStats.baseFF[FFTypes.ANITA] ?: 0.0,
                maxValue = 60,
                width = 90,
            )
        )

        line = if (FFTypes.FARMING_LVL.notSaved()) "§cFarming level not saved\n§eOpen /skills to set it!"
        else "§7§2Fortune for levelling your farming skill\n§2You get 4☘ per farming level"

        content.addTable(
            2,
            GuiRenderUtils.getFarmingBar(
                label = "§2Farming Level",
                tooltip = line,
                currentValue = FFStats.baseFF[FFTypes.FARMING_LVL] ?: 0.0,
                maxValue = 240,
                width = 90,
            )
        )

        line =
            if (FFTypes.COMMUNITY_SHOP.notSaved()) "§cCommunity upgrade level not saved\n§eVisit Elizabeth to set it!"
            else "§7§2Fortune for community shop upgrades\n§2You get 4☘ per upgrade tier"

        content.addTable(
            3,
            GuiRenderUtils.getFarmingBar(
                label = "§2Community upgrades",
                tooltip = line,
                currentValue = FFStats.baseFF[FFTypes.COMMUNITY_SHOP] ?: 0.0,
                maxValue = 40,
                width = 90,
            )
        )

        line =
            if (FFTypes.PLOTS.notSaved()) "§cUnlocked plot count not saved\n§eOpen /desk and view your plots to set it!"
            else "§7§2Fortune for unlocking garden plots\n§2You get 3☘ per plot unlocked"

        content.addTable(
            4,
            GuiRenderUtils.getFarmingBar(
                label = "§2Garden Plots",
                tooltip = line,
                currentValue = FFStats.baseFF[FFTypes.PLOTS] ?: 0.0,
                maxValue = 72,
                width = 90,
            )
        )

        line = when (FFStats.cakeExpireTime) {
            -1L -> "§eYou have not eaten a cake since\n§edownloading this update, assuming the\n§ebuff is active!"
            else -> "§7§2Fortune for eating cake\n§2You get 5☘ for eating cake\n§2Time until cake buff runs out: $timeUntilCakes"
        }
        if (FFStats.cakeExpireTime - System.currentTimeMillis() < 0 && FFStats.cakeExpireTime != -1L) {
            line = "§cYour cake buff has run out\nGo eat some cake!"
        }

        content.addTable(
            5,
            GuiRenderUtils.getFarmingBar(
                label = "§2Cake Buff",
                tooltip = line,
                currentValue = FFStats.baseFF[FFTypes.CAKE] ?: 0.0,
                maxValue = 5,
                width = 90,
            )
        )

        val armorName = FarmingItems.currentArmor?.getItem()?.displayName ?: ""

        armorFF = FarmingItems.currentArmor?.getFFData() ?: FFStats.armorTotalFF

        val wordArmor = if (FarmingItems.currentArmor == null) "Armor" else "Piece"

        line =
            if (FarmingItems.currentArmor == null) "§7§2Total fortune from your armor\n§2Select a piece for more info"
            else "§7§2Total fortune from your\n$armorName"
        var value = if (FarmingItems.currentArmor == null) {
            325
        } else if (FFStats.usingSpeedBoots) {
            when (FarmingItems.currentArmor) {
                FarmingItems.HELMET -> 76.67
                FarmingItems.CHESTPLATE, FarmingItems.LEGGINGS -> 81.67
                else -> 85
            }
        } else {
            when (FarmingItems.currentArmor) {
                FarmingItems.HELMET -> 78.75
                FarmingItems.CHESTPLATE, FarmingItems.LEGGINGS -> 83.75
                else -> 78.75
            }
        }

        content.addTable(
            1,
            GuiRenderUtils.getFarmingBar(
                label = "§2Total $wordArmor Fortune",
                tooltip = line,
                currentValue = armorFF[FFTypes.TOTAL] ?: 0,
                maxValue = value,
                width = 90,
            )
        )

        line =
            if (FarmingItems.currentArmor == null) "§7§2The base fortune from your armor\n§2Select a piece for more info"
            else "§7§2Base fortune from your\n$armorName"
        value = when (FarmingItems.currentArmor) {
            null -> if (FFStats.usingSpeedBoots) 160 else 130
            FarmingItems.HELMET -> 30
            FarmingItems.LEGGINGS -> 35
            FarmingItems.BOOTS -> 35
            else -> if (FFStats.usingSpeedBoots) 60 else 30
        }

        content.addTable(
            2,
            GuiRenderUtils.getFarmingBar(
                label = "§2Base $wordArmor Fortune",
                tooltip = line,
                currentValue = armorFF[FFTypes.BASE] ?: 0,
                maxValue = value + 55,
                width = 90
            )
        )

        line =
            if (FarmingItems.currentArmor == null) "§7§2The fortune from your armor's ability\n§2Select a piece for more info"
            else "§7§2Ability fortune from your\n$armorName"
        value = if (FFStats.usingSpeedBoots) {
            when (FarmingItems.currentArmor) {
                null -> 50
                FarmingItems.BOOTS -> 0
                else -> 16.667
            }
        } else {
            when (FarmingItems.currentArmor) {
                null -> 75
                else -> 18.75
            }
        }

        content.addTable(
            3,
            GuiRenderUtils.getFarmingBar(
                label = "§2$wordArmor Ability",
                tooltip = line,
                currentValue = armorFF[FFTypes.ABILITY] ?: 0,
                maxValue = value,
                width = 90
            )
        )

        line =
            if (FarmingItems.currentArmor == null) "§7§2The fortune from your armor's reforge\n§2Select a piece for more info"
            else "§7§2Total fortune from your\n$armorName}"
        value = if (FarmingItems.currentArmor == null) {
            if (FFStats.usingSpeedBoots) 115 else 120
        } else if (FarmingItems.currentArmor == FarmingItems.BOOTS) {
            if (FFStats.usingSpeedBoots) 25 else 30
        } else 30

        content.addTable(
            4,
            GuiRenderUtils.getFarmingBar(
                label = "§2$wordArmor Reforge",
                tooltip = line,
                currentValue = armorFF[FFTypes.REFORGE] ?: 0,
                maxValue = value,
                width = 90
            )
        )

        val currentPet = FarmingItems.currentPet.getFFData()
        val petMaxFF = when (FarmingItems.currentPet) {
            FarmingItems.ELEPHANT -> 210

            FarmingItems.MOOSHROOM_COW -> 217

            FarmingItems.BEE -> 90

            else -> 60
        }

        footer.add(
            GuiRenderUtils.getFarmingBar(
                label = "§2Total Pet Fortune",
                tooltip = "§7§2The total fortune from your pet and its item",
                currentValue = currentPet[FFTypes.TOTAL] ?: 0,
                maxValue = petMaxFF,
                width = 70
            )
        )



        line = when (FFStats.currentPetItem) {
            "GREEN_BANDANA" -> "§7§2The fortune from your pet's item\n§2Grants 4☘ per garden level"
            "YELLOW_BANDANA" -> "§7§2The fortune from your pet's item"
            "MINOS_RELIC" -> "§cGreen Bandana is better for fortune than minos relic!"
            else -> "No fortune boosting pet item"
        }

        footer.add(
            GuiRenderUtils.getFarmingBar(
                label = "§2Pet Item",
                tooltip = line,
                currentValue = currentPet[FFTypes.PET_ITEM] ?: 0,
                maxValue = 60,
                width = 70,
            )
        )

        val wordEquip = if (FarmingItems.currentEquip == null) "Equipment" else "Piece"

        val equipmentName = FarmingItems.currentEquip?.getItem()?.displayName ?: ""

        equipmentFF = FarmingItems.currentEquip?.getFFData() ?: FFStats.equipmentTotalFF

        val maxEquipmentBaseFortune = 5.0
        val maxEquipmentAbilityFortune = 15.0
        val maxEquipmentReforgeFortune = 15.0
        val maxGreenThumbFortune = GardenAPI.totalAmountVisitorsExisting.toDouble() / 4

        val maxFortunePerPiece =
            maxEquipmentBaseFortune + maxEquipmentAbilityFortune + maxEquipmentReforgeFortune + maxGreenThumbFortune

        line =
            if (FarmingItems.currentEquip == null) "§7§2Total fortune from all your equipment\n§2Select a piece for more info"
            else "§7§2Total fortune from your\n$equipmentName"

        content.addTable(
            1,
            GuiRenderUtils.getFarmingBar(
                label = "§2Total $wordEquip Fortune",
                tooltip = line,
                currentValue = equipmentFF[FFTypes.TOTAL] ?: 0,
                maxValue = if (FarmingItems.currentEquip == null) maxFortunePerPiece * 4 else maxFortunePerPiece,
                width = 90
            )
        )

        line =
            if (FarmingItems.currentEquip == null) "§7§2The base fortune from all your equipment\n§2Select a piece for more info"
            else "§7§2Total base fortune from your\n$equipmentName"

        content.addTable(
            2,
            GuiRenderUtils.getFarmingBar(
                label = "§2$wordEquip Base Fortune",
                tooltip = line,
                currentValue = equipmentFF[FFTypes.BASE] ?: 0,
                maxValue = if (FarmingItems.currentEquip == null) maxEquipmentBaseFortune * 4 else maxEquipmentBaseFortune,
                width = 90,
            )
        )

        line =
            if (FarmingItems.currentEquip == null) "§7§2The fortune from all of your equipment's abilities\n§2Select a piece for more info"
            else "§7§2Total ability fortune from your\n$equipmentName"
        content.addTable(
            3,
            GuiRenderUtils.getFarmingBar(
                label = "§2$wordEquip Ability",
                tooltip = line,
                currentValue = equipmentFF[FFTypes.ABILITY] ?: 0,
                maxValue = if (FarmingItems.currentEquip == null) maxEquipmentAbilityFortune * 4 else maxEquipmentAbilityFortune,
                width = 90,
            )
        )

        line =
            if (FarmingItems.currentEquip == null) "§7§2The fortune from all of your equipment's reforges\n§2Select a piece for more info"
            else "§7§2Total reforge fortune from your\n$equipmentName"

        content.addTable(
            4,
            GuiRenderUtils.getFarmingBar(
                label = "§2$wordEquip Reforge",
                tooltip = line,
                currentValue = equipmentFF[FFTypes.REFORGE] ?: 0,
                maxValue = if (FarmingItems.currentEquip == null) maxEquipmentReforgeFortune * 4 else maxEquipmentReforgeFortune,
                width = 90,
            )
        )

        line =
            if (FarmingItems.currentEquip == null) "§7§2The fortune from all of your equipment's enchantments\n§2Select a piece for more info"
            else "§7§2Total enchantment fortune from your\n${FarmingItems.currentEquip!!.getItem().displayName}"

        content.addTable(
            5,
            Renderable.horizontalContainer(
                FarmingItems.getPetsDisplay(true),
                4,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER
            )
        )

        content.addTable(
            5,
            GuiRenderUtils.getFarmingBar(
                label = "§2$wordEquip Enchantment",
                tooltip = line,
                currentValue = equipmentFF[FFTypes.GREEN_THUMB] ?: 0,
                maxValue = if (FarmingItems.currentEquip == null) maxGreenThumbFortune * 4 else maxGreenThumbFortune,
                width = 90,
            )
        )

        // Displays

        content.addTable(
            0,
            Renderable.horizontalContainer(
                FarmingItems.getArmorDisplay(true),
                4,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER
            )
        )
        content.addTable(
            0,
            Renderable.horizontalContainer(
                FarmingItems.getEquipmentDisplay(true),
                4,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER
            )
        )


        return content to footer
    }

    private fun FFTypes.notSaved(): Boolean = FFStats.baseFF[this]?.let {
        it < 0.0
    } ?: true
}

private fun MutableList<MutableList<Renderable>>.addTable(row: Int, r: Renderable) {
    this.getOrNull(row)?.add(r) ?: mutableListOf(r).let { this.add(row, it) }
}
