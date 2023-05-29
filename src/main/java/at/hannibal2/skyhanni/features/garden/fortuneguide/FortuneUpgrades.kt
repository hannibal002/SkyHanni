package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI.Companion.currentPet
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI.Companion.getItem
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSuffix
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeName
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isRecombobulated
import net.minecraft.item.ItemStack

object FortuneUpgrades {
    private val equipment = listOf(FarmingItems.NECKLACE, FarmingItems.CLOAK, FarmingItems.BELT, FarmingItems.BRACELET)
    private val armor = listOf(FarmingItems.HELMET, FarmingItems.CHESTPLATE, FarmingItems.LEGGINGS, FarmingItems.BOOTS)

    //todo -1 as item count means ah
    val genericUpgrades = mutableListOf<FortuneUpgrade>()

    //todo ironman mode & stranded mode
    // symbol when this is activated, set by default
    // toggle to show upgrades that cannot be bought. Stranded still no recomb or gold ball etc.
    // set visitors served by checking it on equipment
    // not sure if i want to use copper price or bz price for stuff like green thumb

    //todo worry about sorting later, as this may even be in the menu
    fun generateGenericUpgrades() {
        val hidden = GardenAPI.config?.fortune ?: return
        genericUpgrades.add(FortuneUpgrade("Unlock your ${(hidden.plotsUnlocked + 1).addSuffix()} plot",
            null, "COMPOST", compostNeeded[hidden.plotsUnlocked], 3.0))
        genericUpgrades.add(FortuneUpgrade("Upgrade anita bonus to level ${hidden.anitaUpgrade + 1}",
            null, "JACOBS_TICKET", anitaTicketsNeeded[hidden.anitaUpgrade], 3.0))

        getEquipmentUpgrades()
        getArmorUpgrades()
        getPetUpgrades()

// test message for now

        for (a in genericUpgrades) {
            println(a)
        }
    }

    private fun getEquipmentUpgrades() {
        val visitors = GardenAPI.config?.fortune?.uniqueVisitors?.toDouble() ?: 0.0
        for (piece in equipment) {
            val item = piece.getItem()
            val enchantments = item.getEnchantments() ?: emptyMap()
            val greenThumbLvl = (enchantments["green_thumb"] ?: 0)
            if (greenThumbLvl != 5) {
                //todo maybe suggest higher tier books instead of all t1
                // if do this make sure there is ironman/stranded support
                genericUpgrades.add(FortuneUpgrade("Upgrade your ${item.displayName} to green thumb ${greenThumbLvl + 1}",
                    1500, "GREEN_THUMB;1", getNeededBooks(greenThumbLvl), visitors * 0.05))
            }

            when (item.getReforgeName()) {
                "rooted" -> {
                    if (!item.isRecombobulated()) {
                        recombobulateItem(item, 2.0)
                    }
                }
                "blooming" -> {
                    if (!item.isRecombobulated()) {
                        recombobulateItem(item, 1.0)
                        reforgeItem(item, "rooted", "BURROWING_SPORES", 5.0)
                    } else {
                        reforgeItem(item, "rooted", "BURROWING_SPORES", 6.0)
                    }
                }
                else -> {
                    if (!item.isRecombobulated()) {
                        reforgeItem(item, "blooming", "FLOWERING_BOUQUET", 3.0)
                    } else {
                        reforgeItem(item, "blooming", "FLOWERING_BOUQUET", 4.0)
                    }
                }
            }
        }
    }

    private fun getArmorUpgrades() {
        // todo add upgrading armor sets
        // for reforge can do rarity index * multiplier
        // will be linear scale (melon -> cropie -> squash -> fermento)
        // if it is farming boots will keep that line and vice versa
        // will use ah price not material cost
        for (piece in armor) {
            val item = piece.getItem()
            when (item.getReforgeName()) {
                "mossy" -> {
                    if (!item.isRecombobulated()) {
                        recombobulateItem(item, 5.0)
                    }
                }
                "bustling" -> {
                    if (!item.isRecombobulated()) {
                        recombobulateItem(item, 2.0)
                        reforgeItem(item, "mossy", "OVERGROWN_GRASS", 17.0)
                    } else {
                        reforgeItem(item, "mossy", "OVERGROWN_GRASS", 20.0)
                    }
                }
                else -> {
                    if (!item.isRecombobulated()) {
                        reforgeItem(item, "bustling", "SKYMART_BROCHURE", 8.0)
                    } else {
                        reforgeItem(item, "bustling", "SKYMART_BROCHURE", 10.0)
                    }
                }
            }
        }
    }

    //todo needs to be called when switching pets
    private fun getPetUpgrades() {
        val gardenLvl = GardenAPI.getLevelForExp((GardenAPI.config?.experience ?: -1).toLong())

        if (currentPet.getItem().getInternalName().contains(";")) {
            when (FFStats.currentPetItem) {
                "GREEN_BANDANA" -> {}
                "YELLOW_BANDANA" -> {
                    genericUpgrades.add(FortuneUpgrade("Give your ${currentPet.getItem().displayName} a green bandana",
                        null, "GREEN_BANDANA", -1, (4.0 * gardenLvl).coerceAtMost(60.0)))
                }
                else -> {
                    genericUpgrades.add(FortuneUpgrade("Give your ${currentPet.getItem().displayName} a yellow bandana",
                        300, null, null, 30.0))
                }
            }
        }
        // not ironman or stranded friendly
        // assuming stats for having a lvl 100 pet
        if (currentPet == FarmingItems.ELEPHANT && currentPet.getItem().getInternalName() != "ELEPHANT;4") {
            genericUpgrades.add(FortuneUpgrade("Purchase a legendary elephant pet",
                null, "ELEPHANT;4", -1, 180.0))
        }
        else if (currentPet == FarmingItems.MOOSHROOM_COW && currentPet.getItem().getInternalName() != "MOOSHROOM_COW;4") {
            val strength = (GardenAPI.config?.fortune?.farmingStrength)?.toDouble() ?: 0.0
            // using their current strength
            genericUpgrades.add(FortuneUpgrade("Purchase a legendary mooshroom cow pet",
                null, "MOOSHROOM_COW;4", -1, 110.0 + strength / 20.0))
        }


    }

    private fun recombobulateItem(item: ItemStack, increase: Double) {
        genericUpgrades.add(FortuneUpgrade("Recombobulate your ${item.displayName}",
            null, "RECOMBOBULATOR_3000", 1, increase))
    }

    private fun reforgeItem(item: ItemStack, reforgeName: String, reforgeItem: String, increase: Double, copperPrice: Int? = null) {
        genericUpgrades.add(FortuneUpgrade("Reforge your ${item.displayName} to $reforgeName",
            copperPrice, reforgeItem, 1, increase))
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

    // If they unlock in a weird order e.g. getting a corner before a cheaper one won't work properly
    private val compostNeeded = listOf(1, 2, 4, 8, 16, 24, 32, 48, 64, 96, 128, 160, 160,
        320, 320, 480, 480, 640, 800, 1120, 1280, 1600, 1920, 2400)

    // no support for people with 5% discount
    private val anitaTicketsNeeded = listOf(0, 50, 50, 100, 100, 150, 150, 200, 200, 250, 300, 350, 400, 450, 1000)
}