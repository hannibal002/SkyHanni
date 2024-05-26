package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.data.CropAccessoryData
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestones.getCounter
import at.hannibal2.skyhanni.features.garden.CropAccessory
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.CropType.Companion.getTurboCrop
import at.hannibal2.skyhanni.features.garden.FarmingFortuneDisplay
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenAPI.getCropType
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI.Companion.currentPet
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI.Companion.getItem
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrCommon
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil.addSuffix
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getFarmingForDummiesCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeName
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isRecombobulated
import net.minecraft.item.ItemStack

object FortuneUpgrades {

    private val equipment = listOf(FarmingItems.NECKLACE, FarmingItems.CLOAK, FarmingItems.BELT, FarmingItems.BRACELET)
    private val armor = listOf(FarmingItems.HELMET, FarmingItems.CHESTPLATE, FarmingItems.LEGGINGS, FarmingItems.BOOTS)
    private val axeCrops = listOf(CropType.MELON, CropType.PUMPKIN, CropType.COCOA_BEANS)

    val genericUpgrades = mutableListOf<FortuneUpgrade>()
    val cropSpecificUpgrades = mutableListOf<FortuneUpgrade>()

    fun generateGenericUpgrades() {
        val storage = GardenAPI.storage?.fortune ?: return
        genericUpgrades.clear()

        if (storage.plotsUnlocked != -1 && storage.plotsUnlocked != 24) {
            genericUpgrades.add(
                FortuneUpgrade(
                    "§7Unlock your ${(storage.plotsUnlocked + 1).addSuffix()} §7plot",
                    null, "COMPOST", compostNeeded[storage.plotsUnlocked], 3.0
                )
            )
        }
        if (storage.anitaUpgrade != -1 && storage.anitaUpgrade != 15) {
            genericUpgrades.add(
                FortuneUpgrade(
                    "§7Upgrade Anita bonus to level ${storage.anitaUpgrade + 1}",
                    null, "JACOBS_TICKET", anitaTicketsNeeded[storage.anitaUpgrade], 4.0
                )
            )
        }

        getEquipmentUpgrades()
        getPetUpgrades()
        getArmorUpgrades()
        getTalismanUpgrade()

        genericUpgrades.populateAndSort(0)
    }

    // todo fix NEU price data not being loaded if run too early
    private fun MutableList<FortuneUpgrade>.populateAndSort(style: Int) {
        this.map { upgrade ->
            val cost = (upgrade.requiredItem.asInternalName().getPrice() * upgrade.itemQuantity).toInt()
            upgrade.cost = cost
            upgrade.costPerFF = (cost / upgrade.fortuneIncrease).toInt()
        }
        when (style) { // sorting later
            0 -> this.sortBy { it.costPerFF }
            1 -> this.sortByDescending { it.costPerFF }
            2 -> this.sortBy { it.cost }
            3 -> this.sortByDescending { it.cost }
            4 -> this.sortBy { it.fortuneIncrease }
            5 -> this.sortByDescending { it.fortuneIncrease }
            else -> {}
        }
    }

    private fun getTalismanUpgrade() {
        val currentTalismanTier = CropAccessoryData.cropAccessory.ordinal
        if (currentTalismanTier < 3) {
            val nextTalisman = CropAccessory.entries[currentTalismanTier + 1]
            genericUpgrades.add(
                FortuneUpgrade(
                    "§7Upgrade your talisman to ${nextTalisman.internalName?.itemName}",
                    null, nextTalisman.upgradeCost?.first!!, nextTalisman.upgradeCost.second, 10.0
                )
            )
        }
    }

    private fun getEquipmentUpgrades() {
        val visitors = GardenAPI.storage?.uniqueVisitors?.toDouble() ?: 0.0
        for (piece in equipment) {
            val item = piece.getItem()
            // todo tell them to buy the missing item
            if (!item.getInternalName().contains("LOTUS")) return
            val enchantments = item.getEnchantments() ?: emptyMap()
            val greenThumbLvl = enchantments["green_thumb"] ?: 0
            if (greenThumbLvl != 5 && visitors != 0.0) {
                genericUpgrades.add(
                    FortuneUpgrade(
                        "§7Enchant your ${item.displayName} §7with Green Thumb ${greenThumbLvl + 1}",
                        1500, "GREEN_THUMB;1", getNeededBooks(greenThumbLvl), visitors * 0.05
                    )
                )
            }
            recombobulateItem(item, genericUpgrades)
            when (item.getReforgeName()) {
                "rooted" -> {}
                "blooming" -> {
                    reforgeItem(item, FarmingReforges.ROOTED, genericUpgrades)
                }

                else -> {
                    reforgeItem(item, FarmingReforges.BLOOMING, genericUpgrades)
                }
            }
        }
    }
    // todo adding armor tier upgrades later

    private fun getArmorUpgrades() {
        for (piece in armor) {
            val item = piece.getItem()
            // todo skip if it doesnt exist -> tell them to buy it later

            if (FFGuideGUI.isFallbackItem(item)) return

            recombobulateItem(item, genericUpgrades)
            when (item.getReforgeName()) {
                "mossy" -> {}
                "bustling" -> {
                    reforgeItem(item, FarmingReforges.MOSSY, genericUpgrades)
                }

                else -> {
                    reforgeItem(item, FarmingReforges.BUSTLING, genericUpgrades, 100)
                }
            }
        }
    }

    // todo needs to be called when switching pets
    private fun getPetUpgrades() {
        if (currentPet.getItem().getInternalName().contains(";")) {
            when (FFStats.currentPetItem) {
                "GREEN_BANDANA" -> {}
                "YELLOW_BANDANA" -> {
                    // todo once auction stuff is done
                }

                else -> {
                    // give pet yellow bandana
                }
            }
        }
    }

    fun getCropSpecific(tool: ItemStack) {
        cropSpecificUpgrades.clear()
        cropSpecificUpgrades.addAll(genericUpgrades)
        // todo tell them to get the tool if it is missing
        val crop = tool.getCropType() ?: return
        val enchantments = tool.getEnchantments() ?: emptyMap()
        val turboCropLvl = enchantments[crop.getTurboCrop()] ?: 0
        val dedicationLvl = enchantments["dedication"] ?: 0
        val cultivatingLvl = enchantments["cultivating"] ?: 0
        val farmingForDummiesCount = tool.getFarmingForDummiesCount() ?: 0
        if (crop in axeCrops) {
            val sunderLvl = enchantments["sunder"] ?: 0
            if (sunderLvl < 5) {
                cropSpecificUpgrades.add(
                    FortuneUpgrade(
                        "§7Enchant your ${tool.displayName} §7with Sunder ${sunderLvl + 1}",
                        10, "SUNDER;1", getNeededBooks(sunderLvl), 12.5
                    )
                )
            } else if (sunderLvl == 5) {
                cropSpecificUpgrades.add(
                    FortuneUpgrade(
                        "§7Enchant your ${tool.displayName} §7with Sunder 6",
                        10, "SUNDER;6", 1, 12.5
                    )
                )
            }
        } else {
            val harvestingLvl = enchantments["harvesting"] ?: 0
            if (harvestingLvl == 5) {
                cropSpecificUpgrades.add(
                    FortuneUpgrade(
                        "§7Enchant your ${tool.displayName} §7with Harvesting ${harvestingLvl + 1}",
                        10, "HARVESTING;6", 1, 12.5
                    )
                )
            }
        }
        if (farmingForDummiesCount != 5) {
            cropSpecificUpgrades.add(
                FortuneUpgrade(
                    "§7Add a Farming for Dummies to your ${tool.displayName}",
                    null,
                    "FARMING_FOR_DUMMIES",
                    1,
                    1.0
                )
            )
        }
        val cropMilestone = GardenCropMilestones.getTierForCropCount(crop.getCounter(), crop)
        if (dedicationLvl != 4 && cropMilestone > 0) {
            val dedicationMultiplier = listOf(0.5, 0.75, 1.0, 2.0)[dedicationLvl]
            val dedicationIncrease =
                dedicationMultiplier * cropMilestone - FarmingFortuneDisplay.getDedicationFortune(tool, crop)
            if (dedicationLvl == 3) {
                cropSpecificUpgrades.add(
                    FortuneUpgrade(
                        "§7Enchant your ${tool.displayName} §7with Dedication ${dedicationLvl + 1}",
                        null, "DEDICATION;4", 1, dedicationIncrease
                    )
                )
            } else {
                cropSpecificUpgrades.add(
                    FortuneUpgrade(
                        "§7Enchant your ${tool.displayName} §7with Dedication ${dedicationLvl + 1}",
                        250, "DEDICATION;1", getNeededBooks(dedicationLvl), dedicationIncrease
                    )
                )
            }
        }
        if (cultivatingLvl == 0) {
            cropSpecificUpgrades.add(
                FortuneUpgrade("§7Enchant your ${tool.displayName} §7with Cultivating", null, "CULTIVATING;1", 1, 12.0)
            )
        }
        if (turboCropLvl != 5) {
            cropSpecificUpgrades.add(
                FortuneUpgrade(
                    "§7Enchant your ${tool.displayName} §7with ${
                        crop.getTurboCrop().replace("_", " ")
                    } ${turboCropLvl + 1}",
                    null, "${crop.getTurboCrop().uppercase()};1", getNeededBooks(turboCropLvl), 5.0
                )
            )
        }
        recombobulateItem(tool, cropSpecificUpgrades)
        when (tool.getReforgeName()) {
            "blessed" -> {}
            "bountiful" -> {}
            else -> {
                reforgeItem(tool, FarmingReforges.BLESSED, cropSpecificUpgrades)
            }
        }
        cropSpecificUpgrades.populateAndSort(0)
    }

    private fun recombobulateItem(item: ItemStack, list: MutableList<FortuneUpgrade>) {
        if (item.isRecombobulated()) return
        val reforge = item.getReforgeName()?.let {
            FarmingReforges.entries.find { enumValue ->
                enumValue.name == it.uppercase()
            }
        } ?: return

        FarmingFortuneDisplay.loadFortuneLineData(item, 0.0)
        val increase = reforge[item.getItemRarityOrCommon().id + 1, FarmingFortuneDisplay.reforgeFortune] ?: return
        list.add(
            FortuneUpgrade("§7Recombobulate your ${item.displayName}", null, "RECOMBOBULATOR_3000", 1, increase)
        )
    }

    private fun reforgeItem(
        item: ItemStack,
        reforge: FarmingReforges,
        list: MutableList<FortuneUpgrade>,
        copperPrice: Int? = null,
    ) {
        FarmingFortuneDisplay.loadFortuneLineData(item, 0.0)
        val increase = reforge[item.getItemRarityOrCommon().id, FarmingFortuneDisplay.reforgeFortune] ?: return
        list.add(
            FortuneUpgrade(
                "§7Reforge your ${item.displayName} §7to ${reforge.reforgeName}",
                copperPrice, reforge.reforgeItem, 1, increase
            )
        )
    }

    private fun getNeededBooks(currentLvl: Int) = when (currentLvl) {
        0 -> 1
        1 -> 1
        2 -> 2
        3 -> 4
        else -> 8
    }

    private val cropUpgrades = listOf(5, 10, 20, 50, 100, 500, 1000, 5000, 10000)

    // If they unlock in a weird order for example getting a corner before a cheaper one won't work properly
    private val compostNeeded = listOf(
        1, 2, 4, 8, 16, 24, 32, 48, 64, 96, 128, 160, 160,
        320, 320, 480, 480, 640, 800, 1120, 1280, 1600, 1920, 2400
    )

    // no support for people with 5% discount
    private val anitaTicketsNeeded = listOf(0, 50, 50, 100, 100, 150, 150, 200, 200, 250, 300, 350, 400, 450, 1000)
}
