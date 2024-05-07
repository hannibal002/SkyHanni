package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.data.CropAccessoryData
import at.hannibal2.skyhanni.data.GardenCropUpgrades.Companion.getUpgradeLevel
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.FarmingFortuneDisplay
import at.hannibal2.skyhanni.features.garden.GardenAPI
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

    var necklaceFF = mapOf<FFTypes, Double>()
    var cloakFF = mapOf<FFTypes, Double>()
    var beltFF = mapOf<FFTypes, Double>()
    var braceletFF = mapOf<FFTypes, Double>()
    var equipmentTotalFF = mapOf<FFTypes, Double>()

    var helmetFF = mapOf<FFTypes, Double>()
    var chestplateFF = mapOf<FFTypes, Double>()
    var leggingsFF = mapOf<FFTypes, Double>()
    var bootsFF = mapOf<FFTypes, Double>()
    var armorTotalFF = mapOf<FFTypes, Double>()
    var usingSpeedBoots = false

    var elephantFF = mapOf<FFTypes, Double>()
    var mooshroomFF = mapOf<FFTypes, Double>()
    var rabbitFF = mapOf<FFTypes, Double>()
    var beeFF = mapOf<FFTypes, Double>()
    var currentPetItem = ""

    var baseFF = mapOf<FFTypes, Double>()

    var totalBaseFF = mapOf<FFTypes, Double>()

    fun loadFFData() {
        cakeExpireTime = GardenAPI.storage?.fortune?.cakeExpiring ?: -1L

        necklaceFF = getEquipmentFFData(FarmingItems.NECKLACE.getItem())
        cloakFF = getEquipmentFFData(FarmingItems.CLOAK.getItem())
        beltFF = getEquipmentFFData(FarmingItems.BELT.getItem())
        braceletFF = getEquipmentFFData(FarmingItems.BRACELET.getItem())

        equipmentTotalFF =
            (necklaceFF.toList() + cloakFF.toList() + beltFF.toList() + braceletFF.toList()).groupBy({ it.first },
                { it.second }).map { (key, values) -> key to values.sum() }
                .toMap() as MutableMap<FFTypes, Double>

        helmetFF = getArmorFFData(FarmingItems.HELMET.getItem())
        chestplateFF = getArmorFFData(FarmingItems.CHESTPLATE.getItem())
        leggingsFF = getArmorFFData(FarmingItems.LEGGINGS.getItem())
        bootsFF = getArmorFFData(FarmingItems.BOOTS.getItem())

        armorTotalFF =
            (helmetFF.toList() + chestplateFF.toList() + leggingsFF.toList() + bootsFF.toList()).groupBy({ it.first },
                { it.second }).map { (key, values) -> key to values.sum() }
                .toMap() as MutableMap<FFTypes, Double>

        usingSpeedBoots = FarmingItems.BOOTS.getItem()?.getInternalName()?.asString() in farmingBoots

        elephantFF = getPetFFData(FarmingItems.ELEPHANT.getItem())
        mooshroomFF = getPetFFData(FarmingItems.MOOSHROOM_COW.getItem())
        rabbitFF = getPetFFData(FarmingItems.RABBIT.getItem())
        beeFF = getPetFFData(FarmingItems.BEE.getItem())

        baseFF = getGenericFF()

        getTotalFF()
    }

    fun getCropStats(crop: CropType, tool: ItemStack?) {
        FortuneStats.reset()

        FortuneStats.BASE.set(totalBaseFF[FFTypes.TOTAL] ?: 100.0, 1277.0)
        FortuneStats.CROP_UPGRADE.set((crop.getUpgradeLevel()?.toDouble() ?: 0.0) * 5.0, 45.0)
        FortuneStats.ACCESSORY.set(CropAccessoryData.cropAccessory?.getFortune(crop) ?: 0.0, 30.0)
        FortuneStats.FFD.set((tool?.getFarmingForDummiesCount() ?: 0).toDouble(), 5.0)
        FortuneStats.TURBO.set(FarmingFortuneDisplay.getTurboCropFortune(tool, crop), 25.0)
        FortuneStats.DEDICATION.set(FarmingFortuneDisplay.getDedicationFortune(tool, crop), 92.0)
        FortuneStats.CULTIVATING.set(FarmingFortuneDisplay.getCultivatingFortune(tool), 20.0)

        FarmingFortuneDisplay.loadFortuneLineData(tool, 0.0)

        when (crop) {
            in mathCrops -> {
                FortuneStats.BASE_TOOL.set(FarmingFortuneDisplay.getToolFortune(tool), 50.0)
                FortuneStats.COUNTER.set(FarmingFortuneDisplay.getCounterFortune(tool), 96.0)
                FortuneStats.HARVESTING.set(FarmingFortuneDisplay.getHarvestingFortune(tool), 75.0)
                FortuneStats.COLLECTION.set(FarmingFortuneDisplay.getCollectionFortune(tool), 48.0)
                FortuneStats.REFORGE.set(FarmingFortuneDisplay.reforgeFortune, 20.0)
            }

            in dicerCrops -> {
                FortuneStats.SUNDER.set(FarmingFortuneDisplay.getSunderFortune(tool), 75.0)
                FortuneStats.REFORGE.set(FarmingFortuneDisplay.reforgeFortune, 20.0)
            }

            CropType.MUSHROOM -> {
                FortuneStats.BASE_TOOL.set(FarmingFortuneDisplay.getToolFortune(tool), 30.0)
                FortuneStats.HARVESTING.set(FarmingFortuneDisplay.getHarvestingFortune(tool), 75.0)
                FortuneStats.REFORGE.set(FarmingFortuneDisplay.reforgeFortune, 16.0)
            }

            CropType.COCOA_BEANS -> {
                FortuneStats.BASE_TOOL.set(FarmingFortuneDisplay.getToolFortune(tool), 20.0)
                FortuneStats.SUNDER.set(FarmingFortuneDisplay.getSunderFortune(tool), 75.0)
                FortuneStats.REFORGE.set(FarmingFortuneDisplay.reforgeFortune, 16.0)
            }

            CropType.CACTUS -> {
                FortuneStats.HARVESTING.set(FarmingFortuneDisplay.getHarvestingFortune(tool), 75.0)
                FortuneStats.REFORGE.set(FarmingFortuneDisplay.reforgeFortune, 16.0)
            }

            else -> {}
        }
        if (crop == CropType.CARROT) {
            val storage = GardenAPI.storage?.fortune ?: return
            val carrotFortune = if (storage.carrotFortune) 12.0 else 0.0
            FortuneStats.EXPORTED_CARROT.set(carrotFortune, 12.0)
        }
        if (crop == CropType.PUMPKIN) {
            val storage = GardenAPI.storage?.fortune ?: return
            val pumpkinFortune = if (storage.pumpkinFortune) 12.0 else 0.0
            FortuneStats.EXPIRED_PUMPKIN.set(pumpkinFortune, 12.0)
        }

        FortuneStats.CROP_TOTAL.set(FortuneStats.getTotal())
    }

    private fun getEquipmentFFData(item: ItemStack?): Map<FFTypes, Double> = buildMap {
        FarmingFortuneDisplay.loadFortuneLineData(item, 0.0)
        this[FFTypes.BASE] = FarmingFortuneDisplay.itemBaseFortune
        this[FFTypes.REFORGE] = FarmingFortuneDisplay.reforgeFortune
        this[FFTypes.GREEN_THUMB] = FarmingFortuneDisplay.greenThumbFortune
        this[FFTypes.ABILITY] = FarmingFortuneDisplay.getAbilityFortune(item)
        this[FFTypes.TOTAL] = this.values.sum()
    }

    private fun getArmorFFData(item: ItemStack?): Map<FFTypes, Double> = buildMap {
        FarmingFortuneDisplay.loadFortuneLineData(item, 0.0)
        this[FFTypes.BASE] = FarmingFortuneDisplay.itemBaseFortune
        this[FFTypes.REFORGE] = FarmingFortuneDisplay.reforgeFortune
        this[FFTypes.ABILITY] = FarmingFortuneDisplay.getAbilityFortune(item)
        this[FFTypes.TOTAL] = this.values.sum()
    }

    private fun getPetFFData(item: ItemStack?): Map<FFTypes, Double> = buildMap {
        val gardenLvl = GardenAPI.getGardenLevel(overflow = false)
        this[FFTypes.TOTAL] = 0.0
        this[FFTypes.BASE] = getPetFF(item)
        this[FFTypes.PET_ITEM] = when (item?.getPetItem()) {
            "GREEN_BANDANA" -> 4.0 * gardenLvl
            "YELLOW_BANDANA" -> 30.0
            "MINOS_RELIC" -> (this[FFTypes.BASE] ?: 0.0) * .33
            else -> 0.0
        }
        this[FFTypes.TOTAL] = this.values.sum()
    }

    private fun getGenericFF(): Map<FFTypes, Double> = buildMap {
        val storage = GardenAPI.storage?.fortune ?: return emptyMap()
        this[FFTypes.BASE_FF] = 100.0
        this[FFTypes.FARMING_LVL] = storage.farmingLevel.toDouble() * 4
        this[FFTypes.COMMUNITY_SHOP] = (ProfileStorageData.playerSpecific?.gardenCommunityUpgrade ?: -1).toDouble() * 4
        this[FFTypes.PLOTS] = storage.plotsUnlocked.toDouble() * 3
        this[FFTypes.ANITA] = storage.anitaUpgrade.toDouble() * 4
        if (cakeExpireTime - System.currentTimeMillis() > 0 || cakeExpireTime == -1L) {
            this[FFTypes.CAKE] = 5.0
        } else {
            this[FFTypes.CAKE] = 0.0
        }
        this[FFTypes.TOTAL] = this.values.sum()
    }

    fun getTotalFF() {
        var petList = mapOf<FFTypes, Double>()
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
        currentPetItem = FFGuideGUI.currentPet.getItem()?.getPetItem().toString()

        totalBaseFF =
            (baseFF.toList() + armorTotalFF.toList() + equipmentTotalFF.toList() + petList.toList()).groupBy({ it.first },
                { it.second }).map { (key, values) -> key to values.sum() }
                .toMap() as MutableMap<FFTypes, Double>
        FFGuideGUI.updateDisplay()
    }

    private fun getPetFF(pet: ItemStack?): Double {
        if (pet == null) return 0.0
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
