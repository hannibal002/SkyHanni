package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.data.CropAccessoryData
import at.hannibal2.skyhanni.data.GardenCropUpgrades.Companion.getUpgradeLevel
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.features.garden.FarmingFortuneDisplay
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenAPI.getCropType
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI.Companion.getItem
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getFarmingForDummiesCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetItem
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.item.ItemStack

object FFStats {
    private val farmingBoots = arrayListOf("RANCHERS_BOOTS", "PUMPKIN_BOOTS")
    val necklaceFF = mutableMapOf<FFTypes, Double>()
    val cloakFF = mutableMapOf<FFTypes, Double>()
    val beltFF = mutableMapOf<FFTypes, Double>()
    val braceletFF = mutableMapOf<FFTypes, Double>()
    var equipmentTotalFF = mutableMapOf<FFTypes, Double>()

    val helmetFF = mutableMapOf<FFTypes, Double>()
    val chestplateFF = mutableMapOf<FFTypes, Double>()
    val leggingsFF = mutableMapOf<FFTypes, Double>()
    val bootsFF = mutableMapOf<FFTypes, Double>()
    var armorTotalFF = mutableMapOf<FFTypes, Double>()
    var usingSpeedBoots = false

    val elephantFF = mutableMapOf<FFTypes, Double>()
    val mooshroomFF = mutableMapOf<FFTypes, Double>()
    val rabbitFF = mutableMapOf<FFTypes, Double>()
    var currentPetItem = ""

    var baseFF = mutableMapOf<FFTypes, Double>()

    var totalBaseFF = mutableMapOf<FFTypes, Double>()

    val wheatFF = mutableMapOf<FFTypes, Double>()
    val carrotFF = mutableMapOf<FFTypes, Double>()
    val potatoFF = mutableMapOf<FFTypes, Double>()
    val caneFF = mutableMapOf<FFTypes, Double>()
    val wartFF = mutableMapOf<FFTypes, Double>()

    fun loadFFData() {
        getEquipmentFFData(FarmingItems.NECKLACE.getItem(), necklaceFF)
        getEquipmentFFData(FarmingItems.CLOAK.getItem(), cloakFF)
        getEquipmentFFData(FarmingItems.BELT.getItem(), beltFF)
        getEquipmentFFData(FarmingItems.BRACELET.getItem(), braceletFF)

        equipmentTotalFF =
            (necklaceFF.toList() + cloakFF.toList() + beltFF.toList() + braceletFF.toList()).groupBy({ it.first },
                { it.second }).map { (key, values) -> key to values.sum() }
                .toMap() as MutableMap<FFTypes, Double>

        getArmorFFData(FarmingItems.HELMET.getItem(), helmetFF)
        getArmorFFData(FarmingItems.CHESTPLATE.getItem(), chestplateFF)
        getArmorFFData(FarmingItems.LEGGINGS.getItem(), leggingsFF)
        getArmorFFData(FarmingItems.BOOTS.getItem(), bootsFF)

        armorTotalFF =
            (helmetFF.toList() + chestplateFF.toList() + leggingsFF.toList() + bootsFF.toList()).groupBy({ it.first },
                { it.second }).map { (key, values) -> key to values.sum() }
                .toMap() as MutableMap<FFTypes, Double>

        usingSpeedBoots = FarmingItems.BOOTS.getItem().getInternalName() in farmingBoots


        getPetFFData(FarmingItems.ELEPHANT.getItem(), elephantFF)
        getPetFFData(FarmingItems.MOOSHROOM_COW.getItem(), mooshroomFF)
        getPetFFData(FarmingItems.RABBIT.getItem(), rabbitFF)

        getGenericFF(baseFF)
        getToolFF(FarmingItems.WHEAT.getItem(), wheatFF)
        getToolFF(FarmingItems.CARROT.getItem(), carrotFF)
        getToolFF(FarmingItems.POTATO.getItem(), potatoFF)
        getToolFF(FarmingItems.CANE.getItem(), caneFF)
        getToolFF(FarmingItems.NETHER_WART.getItem(), wartFF)

        totalFF(elephantFF)
        currentPetItem = FarmingItems.ELEPHANT.getItem().getPetItem().toString()
    }

    private fun getEquipmentFFData(item: ItemStack, out: MutableMap<FFTypes, Double>) {
        FarmingFortuneDisplay.loadFortuneLineData(item, 0.0)
        out[FFTypes.TOTAL] = 0.0
        out[FFTypes.BASE] = FarmingFortuneDisplay.itemBaseFortune
        out[FFTypes.REFORGE] = FarmingFortuneDisplay.reforgeFortune
        out[FFTypes.GREEN_THUMB] = FarmingFortuneDisplay.greenThumbFortune
        out[FFTypes.ABILITY] = FarmingFortuneDisplay.getAbilityFortune(item)
        out[FFTypes.TOTAL] = out.values.sum()
    }

    private fun getArmorFFData(item: ItemStack, out: MutableMap<FFTypes, Double>) {
        out[FFTypes.TOTAL] = 0.0
        FarmingFortuneDisplay.loadFortuneLineData(item, 0.0)
        out[FFTypes.BASE] = FarmingFortuneDisplay.itemBaseFortune
        out[FFTypes.REFORGE] = FarmingFortuneDisplay.reforgeFortune
        out[FFTypes.ABILITY] = FarmingFortuneDisplay.getAbilityFortune(item)
        out[FFTypes.TOTAL] = out.values.sum()
    }

    //todo mooshroom cow perk
    private fun getPetFFData(item: ItemStack, out: MutableMap<FFTypes, Double>) {
        val gardenLvl = GardenAPI.getLevelForExp((GardenAPI.config?.experience ?: -1).toLong())
        out[FFTypes.TOTAL] = 0.0
        out[FFTypes.BASE] = getPetFF(item)
        out[FFTypes.PET_ITEM] = when (item.getPetItem()) {
            "GREEN_BANDANA" -> (4.0 * gardenLvl).coerceAtMost(60.0)
            "YELLOW_BANDANA" -> 30.0
            "MINOS_RELIC" -> 33.0 //todo
            else -> 0.0
        }
        out[FFTypes.TOTAL] = out.values.sum()
    }

    private fun getGenericFF(out: MutableMap<FFTypes, Double>) {
        val savedStats = GardenAPI.config?.fortune ?: return
        out[FFTypes.TOTAL] = 0.0
        out[FFTypes.BASE_FF] = 100.0
        out[FFTypes.FARMING_LVL] = savedStats.farmingLevel.toDouble() * 4
        out[FFTypes.COMMUNITY_SHOP] = (ProfileStorageData.playerSpecific?.gardenCommunityUpgrade ?: -1).toDouble() * 4
        out[FFTypes.PLOTS] = savedStats.plotsUnlocked.toDouble() * 3
        out[FFTypes.ANITA] = savedStats.anitaUpgrade.toDouble() * 2
        if (savedStats.cakeExpiring - System.currentTimeMillis() > 0 || savedStats.cakeExpiring == -1L) {
            out[FFTypes.CAKE] = 5.0
        } else {
            out[FFTypes.CAKE] = 0.0
        }
        out[FFTypes.TOTAL] = out.values.sum()
    }

    // todo need to add crop upgrades
    private fun getToolFF(tool: ItemStack, out: MutableMap<FFTypes, Double>) {
        out[FFTypes.TOTAL] = 0.0
        val crop = tool.getCropType()

        val accessoryFortune= crop?.let {
            CropAccessoryData.cropAccessory?.getFortune(it)
        }

        out[FFTypes.CROP_UPGRADE] = (crop?.getUpgradeLevel()?.toDouble() ?: 0.0) * 5.0
        out[FFTypes.ACCESSORY] = accessoryFortune ?: 0.0

        out[FFTypes.BASE] = FarmingFortuneDisplay.getToolFortune(tool)
        out[FFTypes.COUNTER] = FarmingFortuneDisplay.getCounterFortune(tool)
        out[FFTypes.COLLECTION] = FarmingFortuneDisplay.getCollectionFortune(tool)
        out[FFTypes.TURBO] = FarmingFortuneDisplay.getTurboCropFortune(tool, crop)
        out[FFTypes.DEDICATION] = FarmingFortuneDisplay.getDedicationFortune(tool, crop)
        out[FFTypes.SUNDER] = FarmingFortuneDisplay.getSunderFortune(tool)
        out[FFTypes.HARVESTING] = FarmingFortuneDisplay.getHarvestingFortune(tool)
        out[FFTypes.CULTIVATING] = FarmingFortuneDisplay.getCultivatingFortune(tool)
        out[FFTypes.FFD] = (tool.getFarmingForDummiesCount() ?: 0).toDouble()

        val enchantmentFortune = out[FFTypes.SUNDER]!! + out[FFTypes.HARVESTING]!! + out[FFTypes.CULTIVATING]!!

        FarmingFortuneDisplay.loadFortuneLineData(tool, enchantmentFortune)

        out[FFTypes.REFORGE] = FarmingFortuneDisplay.reforgeFortune

        out[FFTypes.TOTAL] = out.values.sum()
    }

//todo update the specific crop being looked at
    fun totalFF(petList: MutableMap<FFTypes, Double>) {
        totalBaseFF =
            (baseFF.toList() + armorTotalFF.toList() + equipmentTotalFF.toList() + petList.toList()).groupBy({ it.first },
                { it.second }).map { (key, values) -> key to values.sum() }
                .toMap() as MutableMap<FFTypes, Double>
        currentPetItem = when (FFGuideGUI.currentPet) {
            0 -> FarmingItems.ELEPHANT.getItem().getPetItem().toString()
            1 -> FarmingItems.MOOSHROOM_COW.getItem().getPetItem().toString()
            else -> FarmingItems.RABBIT.getItem().getPetItem().toString()
        }
    }

    private fun getPetFF (pet: ItemStack): Double {
        val petLevelPattern = "§7\\[Lvl (?<level>.*)\\] .*".toPattern()
        var petLevel = 0
        petLevelPattern.matchMatcher(pet.displayName) {
            petLevel = group("level").toInt()
        }

        return if (pet.getInternalName().contains("ELEPHANT;4")) {
            1.8 * petLevel
        } else if (pet.getInternalName().contains("MOOSHROOM")) {
            (10 + petLevel).toDouble()
        } else 0.0
    }
}