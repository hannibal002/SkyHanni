package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.data.CropAccessoryData
import at.hannibal2.skyhanni.data.GardenCropUpgrades.getUpgradeLevel
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.FarmingFortuneDisplay
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI.Companion.getItem
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getFarmingForDummiesCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetItem
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetLevel
import net.minecraft.item.ItemStack
import kotlin.math.floor

object FFStats {

    private val mathCrops =
        listOf(CropType.WHEAT, CropType.CARROT, CropType.POTATO, CropType.SUGAR_CANE, CropType.NETHER_WART)
    private val dicerCrops = listOf(CropType.PUMPKIN, CropType.MELON)

    private val farmingBoots = arrayListOf("RANCHERS_BOOTS", "FARMER_BOOTS")

    var cakeExpireTime = 0L

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
    val beeFF = mutableMapOf<FFTypes, Double>()
    var currentPetItem = ""

    var baseFF = mutableMapOf<FFTypes, Double>()

    var totalBaseFF = mutableMapOf<FFTypes, Double>()

    val cropPage = mutableMapOf<FortuneStats, Pair<Double, Double>>()

    fun loadFFData() {
        cakeExpireTime = GardenAPI.storage?.fortune?.cakeExpiring ?: -1L

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

        usingSpeedBoots = FarmingItems.BOOTS.getItem().getInternalName().asString() in farmingBoots

        getPetFFData(FarmingItems.ELEPHANT.getItem(), elephantFF)
        getPetFFData(FarmingItems.MOOSHROOM_COW.getItem(), mooshroomFF)
        getPetFFData(FarmingItems.RABBIT.getItem(), rabbitFF)
        getPetFFData(FarmingItems.BEE.getItem(), beeFF)

        getGenericFF(baseFF)

        getTotalFF()
    }

    fun getCropStats(crop: CropType, tool: ItemStack) {
        cropPage.clear()
        cropPage[FortuneStats.BASE] = Pair(totalBaseFF[FFTypes.TOTAL] ?: 100.0, if (usingSpeedBoots) 1373.0 else 1377.0)
        cropPage[FortuneStats.CROP_UPGRADE] = Pair((crop.getUpgradeLevel()?.toDouble() ?: 0.0) * 5.0, 45.0)
        cropPage[FortuneStats.ACCESSORY] = Pair(CropAccessoryData.cropAccessory.getFortune(crop), 30.0)
        cropPage[FortuneStats.FFD] = Pair((tool.getFarmingForDummiesCount() ?: 0).toDouble(), 5.0)
        cropPage[FortuneStats.TURBO] = Pair(FarmingFortuneDisplay.getTurboCropFortune(tool, crop), 25.0)
        cropPage[FortuneStats.DEDICATION] = Pair(FarmingFortuneDisplay.getDedicationFortune(tool, crop), 92.0)
        cropPage[FortuneStats.CULTIVATING] = Pair(FarmingFortuneDisplay.getCultivatingFortune(tool), 20.0)

        FarmingFortuneDisplay.loadFortuneLineData(tool, 0.0)

        when (crop) {
            in mathCrops -> {
                cropPage[FortuneStats.BASE_TOOL] = Pair(FarmingFortuneDisplay.getToolFortune(tool), 50.0)
                cropPage[FortuneStats.COUNTER] = Pair(FarmingFortuneDisplay.getCounterFortune(tool), 96.0)
                cropPage[FortuneStats.HARVESTING] = Pair(FarmingFortuneDisplay.getHarvestingFortune(tool), 75.0)
                cropPage[FortuneStats.COLLECTION] = Pair(FarmingFortuneDisplay.getCollectionFortune(tool), 48.0)
                cropPage[FortuneStats.REFORGE] = Pair(FarmingFortuneDisplay.reforgeFortune, 20.0)
                cropPage[FortuneStats.GEMSTONE] = Pair(FarmingFortuneDisplay.gemstoneFortune, 30.0)
            }

            in dicerCrops -> {
                cropPage[FortuneStats.SUNDER] = Pair(FarmingFortuneDisplay.getSunderFortune(tool), 75.0)
                cropPage[FortuneStats.REFORGE] = Pair(FarmingFortuneDisplay.reforgeFortune, 20.0)
                cropPage[FortuneStats.GEMSTONE] = Pair(FarmingFortuneDisplay.gemstoneFortune, 20.0)
            }

            CropType.MUSHROOM -> {
                cropPage[FortuneStats.BASE_TOOL] = Pair(FarmingFortuneDisplay.getToolFortune(tool), 30.0)
                cropPage[FortuneStats.HARVESTING] = Pair(FarmingFortuneDisplay.getHarvestingFortune(tool), 75.0)
                cropPage[FortuneStats.REFORGE] = Pair(FarmingFortuneDisplay.reforgeFortune, 16.0)
                cropPage[FortuneStats.GEMSTONE] = Pair(FarmingFortuneDisplay.gemstoneFortune, 16.0)
            }

            CropType.COCOA_BEANS -> {
                cropPage[FortuneStats.BASE_TOOL] = Pair(FarmingFortuneDisplay.getToolFortune(tool), 20.0)
                cropPage[FortuneStats.SUNDER] = Pair(FarmingFortuneDisplay.getSunderFortune(tool), 75.0)
                cropPage[FortuneStats.REFORGE] = Pair(FarmingFortuneDisplay.reforgeFortune, 16.0)
                cropPage[FortuneStats.GEMSTONE] = Pair(FarmingFortuneDisplay.gemstoneFortune, 16.0)
            }

            CropType.CACTUS -> {
                cropPage[FortuneStats.HARVESTING] = Pair(FarmingFortuneDisplay.getHarvestingFortune(tool), 75.0)
                cropPage[FortuneStats.REFORGE] = Pair(FarmingFortuneDisplay.reforgeFortune, 16.0)
                cropPage[FortuneStats.GEMSTONE] = Pair(FarmingFortuneDisplay.gemstoneFortune, 16.0)
            }

            else -> {}
        }
        if (crop == CropType.CARROT) {
            val storage = GardenAPI.storage?.fortune ?: return
            val carrotFortune = if (storage.carrotFortune) 12.0 else 0.0
            cropPage[FortuneStats.EXPORTED_CARROT] = Pair(carrotFortune, 12.0)
        }
        if (crop == CropType.PUMPKIN) {
            val storage = GardenAPI.storage?.fortune ?: return
            val pumpkinFortune = if (storage.pumpkinFortune) 12.0 else 0.0
            cropPage[FortuneStats.EXPIRED_PUMPKIN] = Pair(pumpkinFortune, 12.0)
        }
        if (crop == CropType.COCOA_BEANS) {
            val storage = GardenAPI.storage?.fortune ?: return
            val cocoaBeansFortune = if (storage.cocoaBeansFortune) 12.0 else 0.0
            cropPage[FortuneStats.SUPREME_CHOCOLATE_BAR] = Pair(cocoaBeansFortune, 12.0)
        }

        cropPage[FortuneStats.CROP_TOTAL] = Pair(
            cropPage.toList().sumOf { it.second.first },
            cropPage.toList().sumOf { it.second.second })
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
        FarmingFortuneDisplay.loadFortuneLineData(item, 0.0)
        out[FFTypes.TOTAL] = 0.0
        out[FFTypes.BASE] = FarmingFortuneDisplay.itemBaseFortune
        out[FFTypes.REFORGE] = FarmingFortuneDisplay.reforgeFortune
        out[FFTypes.GEMSTONE] = FarmingFortuneDisplay.gemstoneFortune
        out[FFTypes.PESTERMINATOR] = FarmingFortuneDisplay.pesterminatorFortune
        out[FFTypes.ABILITY] = FarmingFortuneDisplay.getAbilityFortune(item)
        out[FFTypes.TOTAL] = out.values.sum()
    }

    private fun getPetFFData(item: ItemStack, out: MutableMap<FFTypes, Double>) {
        val gardenLvl = GardenAPI.getGardenLevel(overflow = false)
        out[FFTypes.TOTAL] = 0.0
        out[FFTypes.BASE] = getPetFF(item)
        out[FFTypes.PET_ITEM] = when (item.getPetItem()) {
            "GREEN_BANDANA" -> 4.0 * gardenLvl
            "YELLOW_BANDANA" -> 30.0
            "MINOS_RELIC" -> (out[FFTypes.BASE] ?: 0.0) * .33
            else -> 0.0
        }
        out[FFTypes.TOTAL] = out.values.sum()
    }

    private fun getGenericFF(out: MutableMap<FFTypes, Double>) {
        val storage = GardenAPI.storage?.fortune ?: return
        out[FFTypes.TOTAL] = 0.0
        out[FFTypes.BASE_FF] = 100.0
        out[FFTypes.FARMING_LVL] = storage.farmingLevel.toDouble() * 4
        out[FFTypes.COMMUNITY_SHOP] = (ProfileStorageData.playerSpecific?.gardenCommunityUpgrade ?: -1).toDouble() * 4
        out[FFTypes.PLOTS] = storage.plotsUnlocked.toDouble() * 3
        out[FFTypes.ANITA] = storage.anitaUpgrade.toDouble() * 4
        if (cakeExpireTime - System.currentTimeMillis() > 0 || cakeExpireTime == -1L) {
            out[FFTypes.CAKE] = 5.0
        } else {
            out[FFTypes.CAKE] = 0.0
        }
        out[FFTypes.TOTAL] = out.values.sum()
    }

    fun getTotalFF() {
        var petList = mutableMapOf<FFTypes, Double>()
        when (FFGuideGUI.currentPet) {
            FarmingItems.ELEPHANT -> {
                petList = elephantFF
            }

            FarmingItems.MOOSHROOM_COW -> {
                petList = mooshroomFF
            }

            FarmingItems.RABBIT -> {
                petList = rabbitFF
            }

            FarmingItems.BEE -> {
                petList = beeFF
            }

            else -> {}
        }
        currentPetItem = FFGuideGUI.currentPet.getItem().getPetItem().toString()

        totalBaseFF =
            (baseFF.toList() + armorTotalFF.toList() + equipmentTotalFF.toList() + petList.toList()).groupBy({ it.first },
                { it.second }).map { (key, values) -> key to values.sum() }
                .toMap() as MutableMap<FFTypes, Double>
    }

    private fun getPetFF(pet: ItemStack): Double {
        val petLevel = pet.getPetLevel()
        val strength = (GardenAPI.storage?.fortune?.farmingStrength)
        if (strength != null) {
            val rawInternalName = pet.getInternalName()
            return when {
                rawInternalName.contains("ELEPHANT;4") -> 1.5 * petLevel
                rawInternalName.contains("MOOSHROOM_COW;4") -> {
                    (10 + petLevel).toDouble() + floor(floor(strength / (40 - petLevel * .2)) * .7)
                }

                rawInternalName.contains("MOOSHROOM") -> (10 + petLevel).toDouble()
                rawInternalName.contains("BEE;2") -> 0.2 * petLevel
                rawInternalName.contains("BEE;3") || rawInternalName.contains("BEE;4") -> 0.3 * petLevel
                else -> 0.0
            }
        }
        return 0.0
    }
}
