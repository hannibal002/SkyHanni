package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.data.CropAccessoryData
import at.hannibal2.skyhanni.data.GardenCropUpgrades.getUpgradeLevel
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

        FortuneStats.BASE.set(totalBaseFF[FFTypes.TOTAL] ?: 100.0, if (usingSpeedBoots) 1373.0 else 1377.0)
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
                FortuneStats.GEMSTONE.set(FarmingFortuneDisplay.gemstoneFortune, 30.0)
            }

            in dicerCrops -> {
                FortuneStats.SUNDER.set(FarmingFortuneDisplay.getSunderFortune(tool), 75.0)
                FortuneStats.REFORGE.set(FarmingFortuneDisplay.reforgeFortune, 20.0)
                FortuneStats.GEMSTONE.set(FarmingFortuneDisplay.gemstoneFortune, 20.0)
            }

            CropType.MUSHROOM -> {
                FortuneStats.BASE_TOOL.set(FarmingFortuneDisplay.getToolFortune(tool), 30.0)
                FortuneStats.HARVESTING.set(FarmingFortuneDisplay.getHarvestingFortune(tool), 75.0)
                FortuneStats.REFORGE.set(FarmingFortuneDisplay.reforgeFortune, 16.0)
                FortuneStats.GEMSTONE.set(FarmingFortuneDisplay.gemstoneFortune, 16.0)
            }

            CropType.COCOA_BEANS -> {
                FortuneStats.BASE_TOOL.set(FarmingFortuneDisplay.getToolFortune(tool), 20.0)
                FortuneStats.SUNDER.set(FarmingFortuneDisplay.getSunderFortune(tool), 75.0)
                FortuneStats.REFORGE.set(FarmingFortuneDisplay.reforgeFortune, 16.0)
                FortuneStats.GEMSTONE.set(FarmingFortuneDisplay.gemstoneFortune, 16.0)
            }

            CropType.CACTUS -> {
                FortuneStats.HARVESTING.set(FarmingFortuneDisplay.getHarvestingFortune(tool), 75.0)
                FortuneStats.REFORGE.set(FarmingFortuneDisplay.reforgeFortune, 16.0)
                FortuneStats.GEMSTONE.set(FarmingFortuneDisplay.gemstoneFortune, 16.0)
            }

            else -> {}
        }
        CarrolynTable.getByCrop(crop)?.let {
            val ff = if (it.get()) 12.0 else 0.0
            FortuneStats.CARROLYN.set(ff, 12.0)
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
        this[FFTypes.GEMSTONE] = FarmingFortuneDisplay.gemstoneFortune
        this[FFTypes.PESTERMINATOR] = FarmingFortuneDisplay.pesterminatorFortune
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

    internal enum class FFInfos(
        val sumTo: FFInfos?,
        private val currentF: () -> Number,
        private val maxF: (FFInfos) -> Number,
    ) {
        UNIVERSAL(null, { totalBaseFF }, FFTypes.TOTAL, {
            val backupArmor = FarmingItems.currentArmor
            val backupEquip = FarmingItems.currentEquip
            FarmingItems.currentArmor = null
            FarmingItems.currentEquip = null
            val total = maxSumToThis(it) + 100
            FarmingItems.currentArmor = backupArmor
            FarmingItems.currentEquip = backupEquip
            total
        }),
        ANITA_BUFF(UNIVERSAL, { baseFF }, FFTypes.ANITA, 60),
        FARMING_LEVEL(UNIVERSAL, { baseFF }, FFTypes.FARMING_LVL, 240),
        COMMUNITY_SHOP(UNIVERSAL, { baseFF }, FFTypes.COMMUNITY_SHOP, 40),
        GARDEN_PLOTS(UNIVERSAL, { baseFF }, FFTypes.PLOTS, 72),
        CAKE_BUFF(UNIVERSAL, { baseFF }, FFTypes.CAKE, 5),
        TOTAL_ARMOR(UNIVERSAL, { FarmingItems.currentArmor?.getFFData() ?: armorTotalFF }, FFTypes.TOTAL),
        BASE_ARMOR(TOTAL_ARMOR, { FarmingItems.currentArmor?.getFFData() ?: armorTotalFF }, FFTypes.BASE, {
            when (FarmingItems.currentArmor) {
                FarmingItems.HELMET -> 30
                FarmingItems.CHESTPLATE, FarmingItems.LEGGINGS -> 35
                FarmingItems.BOOTS -> if (usingSpeedBoots) 60 else 30
                else -> if (usingSpeedBoots) 160 else 130
            }
        }),
        ABILITY_ARMOR(TOTAL_ARMOR, { FarmingItems.currentArmor?.getFFData() ?: armorTotalFF }, FFTypes.ABILITY, {
            when (FarmingItems.currentArmor) {
                FarmingItems.HELMET, FarmingItems.CHESTPLATE, FarmingItems.LEGGINGS -> if (usingSpeedBoots) 16.667 else 18.75
                FarmingItems.BOOTS -> if (usingSpeedBoots) 0 else 18.75
                else -> if (usingSpeedBoots) 50 else 75
            }
        }),
        REFORGE_ARMOR(TOTAL_ARMOR, { FarmingItems.currentArmor?.getFFData() ?: armorTotalFF }, FFTypes.REFORGE, {
            when (FarmingItems.currentArmor) {
                FarmingItems.HELMET, FarmingItems.CHESTPLATE, FarmingItems.LEGGINGS -> 30
                FarmingItems.BOOTS -> if (usingSpeedBoots) 25 else 30
                else -> if (usingSpeedBoots) 115 else 120
            }
        }),
        ENCHANT_ARMOR(
            sumTo = TOTAL_ARMOR,
            from = { FarmingItems.currentArmor?.getFFData() ?: armorTotalFF },
            what = FFTypes.PESTERMINATOR,
            x4 = { FarmingItems.currentArmor == null },
            max = 5
        ),
        GEMSTONE_ARMOR(TOTAL_ARMOR, { FarmingItems.currentArmor?.getFFData() ?: armorTotalFF }, FFTypes.GEMSTONE, {
            when (FarmingItems.currentArmor) {
                FarmingItems.HELMET, FarmingItems.CHESTPLATE, FarmingItems.LEGGINGS -> 20
                FarmingItems.BOOTS -> if (usingSpeedBoots) 16 else 20
                else -> if (usingSpeedBoots) 76 else 80
            }
        }),
        TOTAL_PET(UNIVERSAL, { FarmingItems.currentPet.getFFData() }, FFTypes.TOTAL),
        PET_BASE(TOTAL_PET, { FarmingItems.currentPet.getFFData() }, FFTypes.BASE, {
            when (FarmingItems.currentPet) {
                FarmingItems.ELEPHANT -> 150
                FarmingItems.MOOSHROOM_COW -> 157
                FarmingItems.BEE -> 30
                else -> 0
            }
        }),
        PET_ITEM(TOTAL_PET, { FarmingItems.currentPet.getFFData() }, FFTypes.PET_ITEM, 60),
        TOTAL_EQUIP(
            sumTo = UNIVERSAL,
            from = { FarmingItems.currentEquip?.getFFData() ?: equipmentTotalFF },
            what = FFTypes.TOTAL
        ),
        BASE_EQUIP(
            sumTo = TOTAL_EQUIP,
            from = { FarmingItems.currentEquip?.getFFData() ?: equipmentTotalFF },
            what = FFTypes.BASE,
            x4 = { FarmingItems.currentEquip == null },
            max = 5.0
        ),
        ABILITY_EQUIP(
            sumTo = TOTAL_EQUIP,
            from = { FarmingItems.currentEquip?.getFFData() ?: equipmentTotalFF },
            what = FFTypes.ABILITY,
            x4 = { FarmingItems.currentEquip == null },
            max = 15.0
        ),
        REFORGE_EQUIP(
            sumTo = TOTAL_EQUIP,
            from = { FarmingItems.currentEquip?.getFFData() ?: equipmentTotalFF },
            what = FFTypes.REFORGE,
            x4 = { FarmingItems.currentEquip == null },
            max = 15.0
        ),
        ENCHANT_EQUIP(
            sumTo = TOTAL_EQUIP,
            from = { FarmingItems.currentEquip?.getFFData() ?: equipmentTotalFF },
            what = FFTypes.GREEN_THUMB,
            x4 = { FarmingItems.currentEquip == null },
            max = { GardenAPI.totalAmountVisitorsExisting.toDouble() / 4.0 }
        ),
        ;

        val current get() = currentF().toDouble()
        val max get() = maxF(this).toDouble()

        fun bar(label: String, tooltip: String) = GuiRenderUtils.getFarmingBar(label, tooltip, current, max, 90)

        constructor(sumTo: FFInfos?, current: () -> Number, max: Number) : this(sumTo, current, { max })
        constructor(sumTo: FFInfos?, from: () -> Map<FFTypes, Double>, what: FFTypes, max: Number) : this(sumTo, {
            from()[what] ?: 0.0
        }, { max }
        )

        constructor(
            sumTo: FFInfos?,
            from: () -> Map<FFTypes, Double>,
            what: FFTypes,
            x4: () -> Boolean,
            max: Number,
        ) : this(sumTo, { from()[what] ?: 0.0 }, { if (x4()) max.toDouble() * 4 else max }
        )

        constructor(
            sumTo: FFInfos?,
            from: () -> Map<FFTypes, Double>,
            what: FFTypes,
            x4: () -> Boolean,
            max: () -> Number,
        ) : this(sumTo, { from()[what] ?: 0.0 }, { if (x4()) max().toDouble() * 4 else max() }
        )

        constructor(
            sumTo: FFInfos?,
            from: () -> Map<FFTypes, Double>,
            what: FFTypes,
            max: (FFInfos) -> Number,
        ) : this(
            sumTo, { from()[what] ?: 0.0 }, max
        )

        constructor(
            sumTo: FFInfos?,
            from: () -> Map<FFTypes, Double>,
            what: FFTypes,
        ) : this(sumTo, { from()[what] ?: 0.0 }, ::maxSumToThis)

    }

    private fun maxSumToThis(self: FFInfos): Double = FFInfos.entries.filter { it.sumTo == self }.sumOf { it.max }

}
