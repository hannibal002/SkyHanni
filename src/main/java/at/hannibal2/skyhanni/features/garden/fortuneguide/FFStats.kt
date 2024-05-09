package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.data.CropAccessoryData
import at.hannibal2.skyhanni.data.GardenCropUpgrades.Companion.getUpgradeLevel
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.FarmingFortuneDisplay
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
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

    var cakeExpireTime = SimpleTimeMark.farPast()

    var equipmentTotalFF = mapOf<FFTypes, Double>()

    var armorTotalFF = mapOf<FFTypes, Double>()
    var usingSpeedBoots = false

    var currentPetItem = ""

    var baseFF = mapOf<FFTypes, Double>()

    var totalBaseFF = mapOf<FFTypes, Double>()

    fun loadFFData() {
        cakeExpireTime = SimpleTimeMark(GardenAPI.storage?.fortune?.cakeExpiring ?: -1L)

        FarmingItems.resetFFData()

        equipmentTotalFF = FarmingItems.equip.getFFData()

        armorTotalFF = FarmingItems.armor.getFFData()

        usingSpeedBoots = FarmingItems.BOOTS.getItem()?.getInternalName()?.asString() in farmingBoots

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

    fun getEquipmentFFData(item: ItemStack?): Map<FFTypes, Double> = buildMap {
        FarmingFortuneDisplay.loadFortuneLineData(item, 0.0)
        this[FFTypes.BASE] = FarmingFortuneDisplay.itemBaseFortune
        this[FFTypes.REFORGE] = FarmingFortuneDisplay.reforgeFortune
        this[FFTypes.GREEN_THUMB] = FarmingFortuneDisplay.greenThumbFortune
        this[FFTypes.ABILITY] = FarmingFortuneDisplay.getAbilityFortune(item)
        this[FFTypes.TOTAL] = this.values.sum()
    }

    fun getArmorFFData(item: ItemStack?): Map<FFTypes, Double> = buildMap {
        FarmingFortuneDisplay.loadFortuneLineData(item, 0.0)
        this[FFTypes.BASE] = FarmingFortuneDisplay.itemBaseFortune
        this[FFTypes.REFORGE] = FarmingFortuneDisplay.reforgeFortune
        this[FFTypes.ABILITY] = FarmingFortuneDisplay.getAbilityFortune(item)
        this[FFTypes.TOTAL] = this.values.sum()
    }

    fun getPetFFData(item: ItemStack?): Map<FFTypes, Double> = buildMap {
        val gardenLvl = GardenAPI.getGardenLevel(overflow = false)
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
        if (cakeExpireTime.isInPast() || cakeExpireTime.isFarPast()) {
            this[FFTypes.CAKE] = 5.0
        } else {
            this[FFTypes.CAKE] = 0.0
        }
        this[FFTypes.TOTAL] = this.values.sum()
    }

    fun getTotalFF() {

        currentPetItem = FarmingItems.currentPet.getItem()?.getPetItem().toString()

        totalBaseFF = combineFFData(
            baseFF, armorTotalFF, equipmentTotalFF, FarmingItems.currentPet.getFFData()
        )
        FFGuideGUI.updateDisplay()
    }

    fun List<FarmingItems>.getFFData(): Map<FFTypes, Double> = combineFFData(this.map { it.getFFData() })

    fun combineFFData(vararg value: Map<FFTypes, Double>) = combineFFData(value.toList())
    fun combineFFData(value: List<Map<FFTypes, Double>>) =
        value.map { it.toList() }.flatten().groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.sum() }

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

    internal enum class FFInfos(private val currentF: () -> Number, private val maxF: () -> Number) {
        UNIVERSAL({ totalBaseFF }, FFTypes.TOTAL, 1277),
        ANITA_BUFF({ baseFF }, FFTypes.ANITA, 60),
        FARMING_LEVEL({ baseFF }, FFTypes.FARMING_LVL, 240),
        COMMUNITY_SHOP({ baseFF }, FFTypes.COMMUNITY_SHOP, 40),
        GARDEN_PLOTS({ baseFF }, FFTypes.PLOTS, 72),
        CAKE_BUFF({ baseFF }, FFTypes.CAKE, 5),
        TOTAL_ARMOR({ FarmingItems.currentArmor?.getFFData() ?: armorTotalFF }, FFTypes.TOTAL, {
            BASE_ARMOR.max + ABILITY_ARMOR.max + REFORGE_ARMOR.max + REFORGE_ARMOR.max
        }),
        BASE_ARMOR({ FarmingItems.currentArmor?.getFFData() ?: armorTotalFF }, FFTypes.BASE, {
            when (FarmingItems.currentArmor) {
                FarmingItems.HELMET -> 30
                FarmingItems.CHESTPLATE, FarmingItems.LEGGINGS -> 35
                FarmingItems.BOOTS -> if (usingSpeedBoots) 60 else 30
                else -> if (usingSpeedBoots) 160 else 130
            }
        }),
        ABILITY_ARMOR({ FarmingItems.currentArmor?.getFFData() ?: armorTotalFF }, FFTypes.ABILITY, {
            when (FarmingItems.currentArmor) {
                FarmingItems.HELMET, FarmingItems.CHESTPLATE, FarmingItems.LEGGINGS -> if (usingSpeedBoots) 16.667 else 18.75
                FarmingItems.BOOTS -> if (usingSpeedBoots) 0 else 18.75
                else -> if (usingSpeedBoots) 50 else 75
            }
        }),
        REFORGE_ARMOR({ FarmingItems.currentArmor?.getFFData() ?: armorTotalFF }, FFTypes.REFORGE, {
            when (FarmingItems.currentArmor) {
                FarmingItems.HELMET, FarmingItems.CHESTPLATE, FarmingItems.LEGGINGS -> 30
                FarmingItems.BOOTS -> if (usingSpeedBoots) 25 else 30
                else -> if (usingSpeedBoots) 115 else 120
            }
        }),
        TOTAL_PET({ FarmingItems.currentPet.getFFData() }, FFTypes.TOTAL, {
            when (FarmingItems.currentPet) {
                FarmingItems.ELEPHANT -> 150
                FarmingItems.MOOSHROOM_COW -> 157
                FarmingItems.BEE -> 30
                else -> 0
            } + PET_ITEM.max
        }),
        PET_ITEM({ FarmingItems.currentPet.getFFData() }, FFTypes.PET_ITEM, 60),
        TOTAL_EQUIP(
            { FarmingItems.currentEquip?.getFFData() ?: equipmentTotalFF },
            FFTypes.TOTAL,
            { FarmingItems.currentEquip == null },
            { BASE_EQUIP.max + ABILITY_EQUIP.max + REFORGE_EQUIP.max + ENCHANT_EQUIP.max }
        ),
        BASE_EQUIP(
            { FarmingItems.currentEquip?.getFFData() ?: equipmentTotalFF },
            FFTypes.BASE,
            { FarmingItems.currentEquip == null },
            5.0
        ),
        ABILITY_EQUIP(
            { FarmingItems.currentEquip?.getFFData() ?: equipmentTotalFF },
            FFTypes.ABILITY,
            { FarmingItems.currentEquip == null },
            15.0
        ),
        REFORGE_EQUIP(
            { FarmingItems.currentEquip?.getFFData() ?: equipmentTotalFF },
            FFTypes.REFORGE,
            { FarmingItems.currentEquip == null },
            15.0
        ),
        ENCHANT_EQUIP(
            { FarmingItems.currentEquip?.getFFData() ?: equipmentTotalFF },
            FFTypes.GREEN_THUMB,
            { FarmingItems.currentEquip == null },
            { GardenAPI.totalAmountVisitorsExisting.toDouble() / 4.0 }
        ),

        ;

        val current get() = currentF().toDouble()
        val max get() = maxF().toDouble()

        fun bar(label: String, tooltip: String) = GuiRenderUtils.getFarmingBar(label, tooltip, current, max, 90)

        constructor(current: () -> Number, max: Number) : this(current, { max })
        constructor(from: () -> Map<FFTypes, Double>, what: FFTypes, max: Number) : this({ from()[what] ?: 0.0 },
            { max })

        constructor(
            from: () -> Map<FFTypes, Double>,
            what: FFTypes,
            x4: () -> Boolean,
            max: Number,
        ) : this({ from()[what] ?: 0.0 },
            { if (x4()) max.toDouble() * 4 else max })

        constructor(
            from: () -> Map<FFTypes, Double>,
            what: FFTypes,
            x4: () -> Boolean,
            max: () -> Number,
        ) : this({ from()[what] ?: 0.0 },
            { if (x4()) max().toDouble() * 4 else max() })

        constructor(from: () -> Map<FFTypes, Double>, what: FFTypes, max: () -> Number) : this(
            { from()[what] ?: 0.0 }, max
        )
    }
}
