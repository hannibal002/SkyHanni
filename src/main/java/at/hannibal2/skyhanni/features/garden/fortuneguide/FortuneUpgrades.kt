package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.features.garden.FarmingFortuneDisplay
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI.Companion.currentPet
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI.Companion.getItem
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarity
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil.addSuffix
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeName
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isRecombobulated
import net.minecraft.item.ItemStack

object FortuneUpgrades {
    private val equipment = listOf(FarmingItems.NECKLACE, FarmingItems.CLOAK, FarmingItems.BELT, FarmingItems.BRACELET)
    private val armor = listOf(FarmingItems.HELMET, FarmingItems.CHESTPLATE, FarmingItems.LEGGINGS, FarmingItems.BOOTS)

    val genericUpgrades = mutableListOf<FortuneUpgrade>()

    //todo ironman mode & stranded mode
    // symbol when this is activated, set by default
    // toggle to show upgrades that cannot be bought. Stranded still no recomb or gold ball etc.
    // not sure if i want to use copper price or bz price for stuff like green thumb

    //todo worry about sorting later, as this may even be in the menu
    fun generateGenericUpgrades() {
        val hidden = GardenAPI.config?.fortune ?: return
        genericUpgrades.clear()

        if (hidden.plotsUnlocked != -1 && hidden.plotsUnlocked != 24) {
            genericUpgrades.add(FortuneUpgrade("Unlock your ${(hidden.plotsUnlocked + 1).addSuffix()} plot",
                null, "COMPOST", compostNeeded[hidden.plotsUnlocked], 3.0))
        }
        if (hidden.anitaUpgrade != -1 && hidden.anitaUpgrade != 15) {
            genericUpgrades.add(FortuneUpgrade("Upgrade anita bonus to level ${hidden.anitaUpgrade + 1}",
                null, "JACOBS_TICKET", anitaTicketsNeeded[hidden.anitaUpgrade], 3.0))
        }

        getEquipmentUpgrades()
        getPetUpgrades()
        getArmorUpgrades()

        genericUpgrades.populateAndSort(0)
    }

    //todo fix NEU price data not being loaded if run too early
    private fun MutableList<FortuneUpgrade>.populateAndSort(type: Int) {
        this.map { upgrade ->
            val cost = (NEUItems.getPrice(upgrade.requiredItem ?: "") * (upgrade.itemQuantity ?: 1)).toInt()
            upgrade.cost = cost
            upgrade.costPerFF = (cost / upgrade.fortuneIncrease).toInt()
        }
        when (type) {
            0 -> this.sortBy { it.costPerFF }
            1 -> this.sortByDescending { it.costPerFF }
            2 -> this.sortBy { it.cost }
            3 -> this.sortByDescending { it.cost }
            4 -> this.sortBy { it.fortuneIncrease }
            5 -> this.sortByDescending { it.fortuneIncrease }
            else -> {}
        }
    }

    private fun getEquipmentUpgrades() {
        val visitors = GardenAPI.config?.fortune?.uniqueVisitors?.toDouble() ?: 0.0
        for (piece in equipment) {
            val item = piece.getItem()
            //todo tell them to purchase the missing item
            if (!item.getInternalName().contains("LOTUS")) return
            val enchantments = item.getEnchantments() ?: emptyMap()
            val greenThumbLvl = (enchantments["green_thumb"] ?: 0)
            if (greenThumbLvl != 5) {
                //todo maybe suggest higher tier books instead of all t1
                // if do this make sure there is ironman/stranded support
                genericUpgrades.add(FortuneUpgrade("Upgrade your ${item.displayName} §fto green thumb ${greenThumbLvl + 1}",
                    1500, "GREEN_THUMB;1", getNeededBooks(greenThumbLvl), visitors * 0.05))
            }
            recombobulateItem(item)
            when (item.getReforgeName()) {
                "rooted" -> {}
                "blooming" -> {
                    reforgeItem(item, FarmingReforges.ROOTED)
                }
                else -> {
                    reforgeItem(item, FarmingReforges.BLOOMING)
                }
            }
        }
    }

    //todo adding armor tier upgrades later

    private fun getArmorUpgrades() {
        for (piece in armor) {
            val item = piece.getItem()
            //todo skip if it doesnt exist -> tell them to buy it later

            recombobulateItem(item)
            when (item.getReforgeName()) {
                "mossy" -> {}
                "bustling" -> {
                    reforgeItem(item, FarmingReforges.MOSSY)
                }
                else -> {
                    reforgeItem(item, FarmingReforges.BUSTLING, 100)
                }
            }
        }
    }

    //todo needs to be called when switching pets
    private fun getPetUpgrades() {
//        val gardenLvl = GardenAPI.getLevelForExp((GardenAPI.config?.experience ?: -1).toLong())

        if (currentPet.getItem().getInternalName().contains(";")) {
            when (FFStats.currentPetItem) {
                "GREEN_BANDANA" -> {}
                "YELLOW_BANDANA" -> {
                    //todo once auction stuff is done
//                    genericUpgrades.add(FortuneUpgrade("Give your ${currentPet.getItem().displayName} a green bandana",
//                        null, "GREEN_BANDANA", -1, (4.0 * gardenLvl).coerceAtMost(60.0)))
                }
                else -> {
                    genericUpgrades.add(FortuneUpgrade("Give your ${currentPet.getItem().displayName} §fa yellow bandana",
                        300, null, null, 30.0))
                }
            }
        }

        //todo add later
        // not ironman or stranded friendly
        // assuming stats for having a lvl 100 pet
//        if (currentPet == FarmingItems.ELEPHANT && currentPet.getItem().getInternalName() != "ELEPHANT;4") {
//            genericUpgrades.add(FortuneUpgrade("Purchase a legendary elephant pet",
//                null, "ELEPHANT;4", -1, 180.0))
//        }
//        else if (currentPet == FarmingItems.MOOSHROOM_COW && currentPet.getItem().getInternalName() != "MOOSHROOM_COW;4") {
//            val strength = (GardenAPI.config?.fortune?.farmingStrength)?.toDouble() ?: 0.0
//            genericUpgrades.add(FortuneUpgrade("Purchase a legendary mooshroom cow pet",
//                null, "MOOSHROOM_COW;4", -1, 110.0 + strength / 20.0))
//        }
    }

    private fun recombobulateItem(item: ItemStack) {
        if (item.isRecombobulated()) return
        val reforge = item.getReforgeName()?.let { FarmingReforges.valueOf(it.uppercase()) } ?: return

        FarmingFortuneDisplay.loadFortuneLineData(item, 0.0)
        val increase = reforge[item.getItemRarity() + 1, FarmingFortuneDisplay.reforgeFortune] ?: return
        genericUpgrades.add(FortuneUpgrade("Recombobulate your ${item.displayName}",
            null, "RECOMBOBULATOR_3000", 1, increase))
    }

    private fun reforgeItem(item: ItemStack, reforge: FarmingReforges, copperPrice: Int? = null) {
        FarmingFortuneDisplay.loadFortuneLineData(item, 0.0)

        val increase = reforge[item.getItemRarity(), FarmingFortuneDisplay.reforgeFortune] ?: return
        genericUpgrades.add(FortuneUpgrade("Reforge your ${item.displayName}§f to ${reforge.reforgeName}",
            copperPrice, reforge.reforgeItem, 1, increase))
    }

    private fun getNeededBooks(currentLvl: Int): Int {
        return when (currentLvl) {
            0 -> 1
            1 -> 1
            2 -> 2
            3 -> 4
            else -> 8
        }
    }

    private val cropUpgrades = listOf(5, 10, 20, 50, 100, 500, 1000, 5000, 10000)

    // If they unlock in a weird order e.g. getting a corner before a cheaper one won't work properly
    private val compostNeeded = listOf(1, 2, 4, 8, 16, 24, 32, 48, 64, 96, 128, 160, 160,
        320, 320, 480, 480, 640, 800, 1120, 1280, 1600, 1920, 2400)

    // no support for people with 5% discount
    private val anitaTicketsNeeded = listOf(0, 50, 50, 100, 100, 150, 150, 200, 200, 250, 300, 350, 400, 450, 1000)
}