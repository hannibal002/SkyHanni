package at.hannibal2.skyhanni.features.garden.fortuneguide.pages

import at.hannibal2.skyhanni.features.garden.fortuneguide.FFInfos
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFStats
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFTypes
import at.hannibal2.skyhanni.features.garden.fortuneguide.FarmingItemType
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.guide.GuideTablePage
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical

class OverviewPage(sizeX: Int, sizeY: Int, paddingX: Int = 15, paddingY: Int = 7, footerSpacing: Int = 6) : GuideTablePage(
    sizeX, sizeY, paddingX, paddingY, footerSpacing,
) {

    override fun onEnter() {
        update(
            listOf(
                header(),
                content()
            ),
            footer()
        )
    }

    private fun header(): List<Renderable> = buildList {
        add(
            FFInfos.UNIVERSAL.bar(
                "§6Universal Farming Fortune",
                "§7§2Farming fortune in that is\n§2applied to every crop\n§eNot the same as tab FF\n" +
                    "§eSee on the grass block page",
            )
        )

        add(
            Renderable.horizontal(
                FarmingItemType.getArmorDisplay(true),
                4,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            )
        )

        add(
            Renderable.horizontal(
                FarmingItemType.getEquipmentDisplay(true),
                4,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            )
        )
    }

    // Build content list of farming fortune data
    private fun content(): List<Renderable> = buildList {
        add(universalFortune())
        add(armorFortune())
        add(equipmentFortune())
    }

    private fun universalFortune(): Renderable {
        val timeUntilCakes = FFStats.cakeExpireTime.timeUntil().format(TimeUnit.HOUR, maxUnits = 1)
        return Renderable.vertical(
            FFInfos.FARMING_LEVEL.bar(
                "§2Farming Level",
                if (FFTypes.FARMING_LVL.notSaved()) "§cFarming level not saved\n§eOpen /skills to set it!"
                else "§7§2Fortune for levelling your farming skill\n§2You get 4☘ per farming level",
            ),
            FFInfos.ATTRIBUTE_SHARDS.bar(
                "§2Attribute Shards",
                if (FFTypes.ATTRIBUTE_SHARDS.notSaved()) "§cAttribute Shards not saved\n§eOpen /am to set it!"
                else "§7§2Fortune from attribute shards",
            ),
            FFInfos.BESTIARY.bar(
                "§2Bestiary",
                if (FFTypes.BESTIARY.notSaved()) "§cBestiary fortune not saved\n§eOpen /bestiary to set it!"
                else "§7§2Fortune for killing pests\n§2You get 0.4☘ per bestiary milestone per pest",
            ),
            FFInfos.GARDEN_PLOTS.bar(
                "§2Garden Plots",
                if (FFTypes.PLOTS.notSaved()) "§cUnlocked plot count not saved\n" +
                    "§eOpen /desk and view your plots to set it!"
                else "§7§2Fortune for unlocking garden plots\n§2You get 3☘ per plot unlocked",
            ),
            FFInfos.ANITA_BUFF.bar(
                "§2Anita Buff",
                if (FFTypes.ANITA.notSaved()) "§cAnita buff not saved\n§eVisit Anita to set it!"
                else "§7§2Fortune for levelling your Anita extra crops\n§2You get 4☘ per buff level",
            ),
            FFInfos.COMMUNITY_SHOP.bar(
                "§2Community upgrades",
                if (FFTypes.COMMUNITY_SHOP.notSaved()) "§cCommunity upgrade level not saved\n" +
                    "§eVisit Elizabeth to set it!"
                else "§7§2Fortune for community shop upgrades\n§2You get 4☘ per upgrade tier",
            ),
            FFInfos.DARK_CACAO_TRUFFLE.bar(
                "§2Refined Dark Cacao Truffle",
                if (FFTypes.DARK_CACAO_TRUFFLE.notSaved()) "§Truffle not saved\n" +
                    "\n§eOpen detailed Farming Fortune stats to set it!"
                else "§7§2Global fortune for Refined Dark Cacao Truffle",
            ),
            FFInfos.RELIC_OF_POWER.bar(
                "§2Relic of Power",
                if (FFTypes.RELIC_OF_POWER.notSaved()) "§Accessories not saved\n" +
                    "\n§eOpen Accessory Bag or details Farming Fortune stats to set it!"
                else "§7§2Fortune for Gemstones in Relic of Power",
            ),
            FFInfos.CAKE_BUFF.bar(
                "§2Cake Buff",
                when {
                    FFStats.cakeExpireTime.isFarPast() ->
                        "§eYou have not eaten a cake since\n§edownloading this update, assuming the\n§ebuff is active!"

                    FFStats.cakeExpireTime.isInPast() ->
                        "§cYour cake buff has run out\nGo eat some cake!"

                    else ->
                        "§7§2Fortune for eating cake\n§2You get 5☘ for eating cake\n" +
                            "§2Time until cake buff runs out: $timeUntilCakes"
                },
            ),
            spacing = 2
        )
    }

    private fun armorFortune(): Renderable {
        val moreInfo = "§2Select a piece for more info"
        val wordArmor = if (FarmingItemType.currentArmor == null) "Armor" else "Piece"
        val armorName = FarmingItemType.currentArmor?.getItem()?.displayName.orEmpty()

        return Renderable.vertical(
            FFInfos.TOTAL_ARMOR.bar(
                "§2Total $wordArmor Fortune",
                if (FarmingItemType.currentArmor == null) "§7§2Total fortune from your armor\n$moreInfo"
                else "§7§2Total fortune from your\n$armorName",
            ),
            FFInfos.BASE_ARMOR.bar(
                "§2Base $wordArmor Fortune",
                if (FarmingItemType.currentArmor == null) "§7§2The base fortune from your armor\n$moreInfo"
                else "§7§2Base fortune from your\n$armorName",
            ),
            FFInfos.ABILITY_ARMOR.bar(
                "§2$wordArmor Ability",
                if (FarmingItemType.currentArmor == null) "§7§2The fortune from your armor's ability\n$moreInfo"
                else "§7§2Ability fortune from your\n$armorName",
            ),
            FFInfos.REFORGE_ARMOR.bar(
                "§2$wordArmor Reforge",
                if (FarmingItemType.currentArmor == null) "§7§2The fortune from your armor's reforge\n$moreInfo"
                else "§7§2Reforge fortune from your\n$armorName",
            ),
            FFInfos.ENCHANT_ARMOR.bar(
                "§2$wordArmor Enchantment",
                if (FarmingItemType.currentArmor == null) "§7§2The fortune from your armor's enchantments\n$moreInfo"
                else "§7§2Enchantment fortune from your\n$armorName",
            ),
            FFInfos.GEMSTONE_ARMOR.bar(
                "§2$wordArmor Gemstones",
                if (FarmingItemType.currentArmor == null) "§7§2The fortune from your armor's gemstones\n$moreInfo"
                else "§7§2Gemstone fortune from your\n$armorName",
            ),
            spacing = 2
        )
    }

    private fun equipmentFortune(): Renderable {
        val moreInfo = "§2Select a piece for more info"
        val wordEquip = if (FarmingItemType.currentEquip == null) "Equipment" else "Piece"
        val equipmentName = FarmingItemType.currentEquip?.getItem()?.displayName.orEmpty()

        return Renderable.vertical(
            FFInfos.TOTAL_EQUIP.bar(
                "§2Total $wordEquip Fortune",
                if (FarmingItemType.currentEquip == null) "§7§2Total fortune from your equipment\n$moreInfo"
                else "§7§2Total fortune from your\n$equipmentName",
            ),
            FFInfos.BASE_EQUIP.bar(
                "§2$wordEquip Base Fortune",
                if (FarmingItemType.currentEquip == null) "§7§2The base fortune from your equipment\n$moreInfo"
                else "§7§2Base fortune from your\n$equipmentName",
            ),
            FFInfos.ABILITY_EQUIP.bar(
                "§2$wordEquip Ability",
                if (FarmingItemType.currentEquip == null) "§7§2The fortune from your equipment's abilities\n$moreInfo"
                else "§7§2Ability fortune from your\n$equipmentName",
            ),
            FFInfos.REFORGE_EQUIP.bar(
                "§2$wordEquip Reforge",
                if (FarmingItemType.currentEquip == null) "§7§2The fortune from your equipment's reforges\n$moreInfo"
                else "§7§2Reforge fortune from your\n$equipmentName",
            ),
            FFInfos.ENCHANT_EQUIP.bar(
                "§2$wordEquip Enchantment",
                if (FarmingItemType.currentEquip == null) "§7§2The fortune from your equipment's enchantments\n$moreInfo"
                else "§7§2Enchantment fortune from your\n$equipmentName",
            ),
            spacing = 2
        )
    }

    private fun footer(): List<Renderable> = buildList {
        val footer = mutableListOf<Renderable>()
        val petFooter = Renderable.horizontal(
            FarmingItemType.getPetsDisplay(true),
            4,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            verticalAlign = RenderUtils.VerticalAlignment.CENTER,
        )

        footer.add(
            FFInfos.TOTAL_PET.bar(
                "§2Total Pet Fortune",
                "§7§2The total fortune from your pet and its item",
            ),
        )

        footer.add(
            FFInfos.PET_BASE.bar(
                "§2Base Pet Fortune",
                "§7§2The base fortune from your pet",
            ),
        )

        footer.add(
            FFInfos.PET_ITEM.bar(
                "§2Pet Item",
                when (FFStats.currentPetItem) {
                    "GREEN_BANDANA" -> "§7§2The fortune from your pet's item\n§2Grants 4☘ per garden level"
                    "YELLOW_BANDANA" -> "§7§2The fortune from your pet's item"
                    "MINOS_RELIC" -> "§cGreen Bandana is better for fortune than minos relic!"
                    else -> "No fortune boosting pet item"
                },
            ),
        )

        // Render footer data for pet fortune
        add(
            Renderable.vertical(
                petFooter,
                Renderable.horizontal(
                    footer,
                    spacing = 15,
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER, verticalAlign = RenderUtils.VerticalAlignment.CENTER,
                ),
                spacing = 2,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            )
        )
    }

    private fun FFTypes.notSaved(): Boolean = FFStats.baseFF[this]?.let {
        it < 0.0
    } ?: true
}
