package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.utils.GuiRenderUtils

internal enum class FFInfos(
    val sumTo: FFInfos?,
    private val currentF: () -> Number,
    private val maxF: (FFInfos) -> Number,
) {
    UNIVERSAL(
        null, { FFStats.totalBaseFF }, FFTypes.TOTAL,
        {
            val backupArmor = FarmingItemType.currentArmor
            val backupEquip = FarmingItemType.currentEquip
            FarmingItemType.currentArmor = null
            FarmingItemType.currentEquip = null
            val total = maxSumToThis(it)
            FarmingItemType.currentArmor = backupArmor
            FarmingItemType.currentEquip = backupEquip
            total
        },
    ),
    FARMING_LEVEL(UNIVERSAL, { FFStats.baseFF }, FFTypes.FARMING_LVL, 240),
    BESTIARY(UNIVERSAL, { FFStats.baseFF }, FFTypes.BESTIARY, 66),
    GARDEN_PLOTS(UNIVERSAL, { FFStats.baseFF }, FFTypes.PLOTS, 72),
    ANITA_BUFF(UNIVERSAL, { FFStats.baseFF }, FFTypes.ANITA, 60),
    COMMUNITY_SHOP(UNIVERSAL, { FFStats.baseFF }, FFTypes.COMMUNITY_SHOP, 40),
    CAKE_BUFF(UNIVERSAL, { FFStats.baseFF }, FFTypes.CAKE, 5),
    TOTAL_ARMOR(UNIVERSAL, { FarmingItemType.currentArmor?.getFFData() ?: FFStats.armorTotalFF }, FFTypes.TOTAL),
    BASE_ARMOR(
        TOTAL_ARMOR, { FarmingItemType.currentArmor?.getFFData() ?: FFStats.armorTotalFF }, FFTypes.BASE,
        {
            when (FarmingItemType.currentArmor) {
                FarmingItemType.HELMET -> 30
                FarmingItemType.CHESTPLATE, FarmingItemType.LEGGINGS -> 35
                FarmingItemType.BOOTS -> if (FFStats.usingSpeedBoots) 60 else 30
                else -> if (FFStats.usingSpeedBoots) 160 else 130
            }
        },
    ),
    ABILITY_ARMOR(
        TOTAL_ARMOR, { FarmingItemType.currentArmor?.getFFData() ?: FFStats.armorTotalFF }, FFTypes.ABILITY,
        {
            when (FarmingItemType.currentArmor) {
                FarmingItemType.HELMET,
                FarmingItemType.CHESTPLATE,
                FarmingItemType.LEGGINGS,
                ->
                    if (FFStats.usingSpeedBoots) 16.667 else 18.75

                FarmingItemType.BOOTS -> if (FFStats.usingSpeedBoots) 0 else 18.75
                else -> if (FFStats.usingSpeedBoots) 50 else 75
            }
        },
    ),
    REFORGE_ARMOR(
        TOTAL_ARMOR, { FarmingItemType.currentArmor?.getFFData() ?: FFStats.armorTotalFF }, FFTypes.REFORGE,
        {
            when (FarmingItemType.currentArmor) {
                FarmingItemType.HELMET, FarmingItemType.CHESTPLATE, FarmingItemType.LEGGINGS -> 30
                FarmingItemType.BOOTS -> if (FFStats.usingSpeedBoots) 25 else 30
                else -> if (FFStats.usingSpeedBoots) 115 else 120
            }
        },
    ),
    ENCHANT_ARMOR(
        sumTo = TOTAL_ARMOR,
        from = { FarmingItemType.currentArmor?.getFFData() ?: FFStats.armorTotalFF },
        what = FFTypes.ENCHANT,
        x4 = { FarmingItemType.currentArmor == null },
        max = 12,
    ),
    GEMSTONE_ARMOR(
        TOTAL_ARMOR, { FarmingItemType.currentArmor?.getFFData() ?: FFStats.armorTotalFF }, FFTypes.GEMSTONE,
        {
            when (FarmingItemType.currentArmor) {
                FarmingItemType.HELMET, FarmingItemType.CHESTPLATE, FarmingItemType.LEGGINGS -> 20
                FarmingItemType.BOOTS -> if (FFStats.usingSpeedBoots) 16 else 20
                else -> if (FFStats.usingSpeedBoots) 76 else 80
            }
        },
    ),
    TOTAL_PET(UNIVERSAL, { FarmingItemType.currentPet.getFFData() }, FFTypes.TOTAL),
    PET_BASE(
        TOTAL_PET, { FarmingItemType.currentPet.getFFData() }, FFTypes.BASE,
        {
            when (FarmingItemType.currentPet) {
                FarmingItemType.ELEPHANT -> 150
                FarmingItemType.MOOSHROOM_COW -> 158
                FarmingItemType.BEE -> 30
                FarmingItemType.SLUG -> 100
                FarmingItemType.HEDGEHOG -> 45 * 3
                else -> 0
            }
        },
    ),
    PET_ITEM(TOTAL_PET, { FarmingItemType.currentPet.getFFData() }, FFTypes.PET_ITEM, 60),
    TOTAL_EQUIP(
        sumTo = UNIVERSAL,
        from = { FarmingItemType.currentEquip?.getFFData() ?: FFStats.equipmentTotalFF },
        what = FFTypes.TOTAL,
    ),
    BASE_EQUIP(
        sumTo = TOTAL_EQUIP,
        from = { FarmingItemType.currentEquip?.getFFData() ?: FFStats.equipmentTotalFF },
        what = FFTypes.BASE,
        x4 = { FarmingItemType.currentEquip == null },
        max = 5.0,
    ),
    ABILITY_EQUIP(
        sumTo = TOTAL_EQUIP,
        from = { FarmingItemType.currentEquip?.getFFData() ?: FFStats.equipmentTotalFF },
        what = FFTypes.ABILITY,
        x4 = { FarmingItemType.currentEquip == null },
        max = 15.0,
    ),
    REFORGE_EQUIP(
        sumTo = TOTAL_EQUIP,
        from = { FarmingItemType.currentEquip?.getFFData() ?: FFStats.equipmentTotalFF },
        what = FFTypes.REFORGE,
        x4 = { FarmingItemType.currentEquip == null },
        max = 15.0,
    ),
    ENCHANT_EQUIP(
        sumTo = TOTAL_EQUIP,
        from = { FarmingItemType.currentEquip?.getFFData() ?: FFStats.equipmentTotalFF },
        what = FFTypes.ENCHANT,
        x4 = { FarmingItemType.currentEquip == null },
        max = { at.hannibal2.skyhanni.features.garden.GardenApi.totalAmountVisitorsExisting.toDouble() / 4.0 },
    ),
    ;

    val current get() = currentF().toDouble()
    val max get() = maxF(this).toDouble()

    fun bar(label: String, tooltip: String, width: Int = 90) =
        GuiRenderUtils.getFarmingBar(label, tooltip, current, max, width)

    constructor(
        sumTo: FFInfos?,
        current: () -> Number,
        max: Number,
    ) : this(sumTo, current, { max })

    constructor(
        sumTo: FFInfos?,
        from: () -> Map<FFTypes, Double>,
        what: FFTypes,
        max: Number,
    ) : this(sumTo, { from()[what] ?: 0.0 }, { max })

    constructor(
        sumTo: FFInfos?,
        from: () -> Map<FFTypes, Double>,
        what: FFTypes,
        x4: () -> Boolean,
        max: Number,
    ) : this(sumTo, { from()[what] ?: 0.0 }, { if (x4()) max.toDouble() * 4 else max })

    constructor(
        sumTo: FFInfos?,
        from: () -> Map<FFTypes, Double>,
        what: FFTypes,
        x4: () -> Boolean,
        max: () -> Number,
    ) : this(sumTo, { from()[what] ?: 0.0 }, { if (x4()) max().toDouble() * 4 else max() })

    constructor(
        sumTo: FFInfos?,
        from: () -> Map<FFTypes, Double>,
        what: FFTypes,
        max: (FFInfos) -> Number,
    ) : this(sumTo, { from()[what] ?: 0.0 }, max)

    constructor(
        sumTo: FFInfos?,
        from: () -> Map<FFTypes, Double>,
        what: FFTypes,
    ) : this(sumTo, { from()[what] ?: 0.0 }, ::maxSumToThis)
}

private fun maxSumToThis(self: FFInfos): Double = FFInfos.entries.filter { it.sumTo == self }.sumOf { it.max }
