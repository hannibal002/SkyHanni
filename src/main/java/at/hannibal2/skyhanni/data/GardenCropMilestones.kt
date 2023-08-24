package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.CropMilestoneUpdateEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenCropMilestones {
    private val cropPattern = "§7Harvest §f(?<name>.*) §7on .*".toPattern()
    private val totalPattern = "§7Total: §a(?<name>.*)".toPattern()

    // Add when api support is there
//    @SubscribeEvent
//    fun onProfileDataLoad(event: ProfileApiDataLoadedEvent) {
//        val profileData = event.profileData
//        for ((key, value) in profileData.entrySet()) {
//            if (key.startsWith("experience_skill_")) {
//                val label = key.substring(17)
//                val exp = value.asLong
//                gardenExp[label] = exp
//            }
//        }
//    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Crop Milestones") return

        for ((_, stack) in event.inventoryItems) {
            var crop: CropType? = null
            for (line in stack.getLore()) {
                cropPattern.matchMatcher(line) {
                    val name = group("name")
                    crop = CropType.getByNameOrNull(name)
                }
                totalPattern.matchMatcher(line) {
                    val amount = group("name").replace(",", "").toLong()
                    crop?.setCounter(amount)
                }
            }
        }
        CropMilestoneUpdateEvent().postAndCatch()
    }

    companion object {
        val cropCounter: MutableMap<CropType, Long>? get() = GardenAPI.config?.cropCounter

        // TODO make nullable
        fun CropType.getCounter() = cropCounter?.get(this) ?: 0

        fun CropType.setCounter(counter: Long) {
            cropCounter?.set(this, counter)
        }

        fun CropType.isMaxed() = getCounter() >= 1_000_000_000

        fun getTierForCrops(crops: Long): Int {
            var tier = 0
            var totalCrops = 0L
            for (tierCrops in cropMilestone) {
                totalCrops += tierCrops
                if (totalCrops > crops) {
                    return tier
                }
                tier++
            }

            return tier
        }

        fun getCropsForTier(requestedTier: Int): Long {
            var totalCrops = 0L
            var tier = 0
            for (tierCrops in cropMilestone) {
                totalCrops += tierCrops
                tier++
                if (tier == requestedTier) {
                    return totalCrops
                }
            }

            return 0
        }

        fun CropType.progressToNextLevel(): Double {
            val progress = getCounter()
            val startTier = getTierForCrops(progress)
            val startCrops = getCropsForTier(startTier)
            val end = getCropsForTier(startTier + 1).toDouble()
            return (progress - startCrops) / (end - startCrops)
        }

        // TODO use repo
        private val cropMilestone = listOf(
            100,
            150,
            250,
            500,
            1500,
            2500,
            5000,
            5000,
            10000,
            25000,
            25000,
            25000,
            30000,
            70000,
            100000,
            200000,
            250000,
            250000,
            500000,
            1000000,
            1500000,
            2000000,
            3000000,
            4000000,
            7000000,
            10000000,
            20000000,
            25000000,
            25000000,
            50000000,
            50000000,
            50000000,
            50000000,
            50000000,
            50000000,
            50000000,
            50000000,
            50000000,
            50000000,
            50000000,
            50000000,
            50000000,
            50000000,
            50000000,
            50000000,
            100000000,
        )
        // Current milestone list from https://wiki.hypixel.net/Template:Crop_Milestones
        private val wheatMilestone = listOf(
            30,
            80,
            160,
            330,
            1630,
            3630,
            5630,
            8630,
            16630,
            24630,
            32630,
            42630,
            62630,
            92630,
            162630,
            242630,
            322630,
            492630,
            822630,
            1322630,
            1982630,
            2982630,
            4282630,
            6582630,
            9882630,
            14882630,
            19882630,
            24882630,
            29882630,
            34882630,
            39882630,
            44882630,
            49882630,
            54882630,
            59882630,
            64882630,
            69882630,
            74882630,
            79882630,
            84882630,
            89882630,
            94882630,
            99882630,
            104882630,
            109882630,
        )
        private val carrotMilestone = listOf(
            100,
            250,
            500,
            1000,
            2500,
            5000,
            10000,
            15000,
            25000,
            50000,
            75000,
            100000,
            130000,
            200000,
            300000,
            500000,
            750000,
            100000,
            1500000,
            2500000,
            4000000,
            6000000,
            9000000,
            13000000,
            20000000,
            30000000,
            45000000,
            60000000,
            75000000,
            90000000,
            105000000,
            120000000,
            135000000,
            150000000,
            165000000,
            180000000,
            195000000,
            210000000,
            225000000,
            240000000,
            255000000,
            270000000,
            285000000,
            300000000,
            315000000,
            330000000
        )
        private val potatoMilestone = listOf(
            100,
            250,
            550,
            1050,
            2550,
            5550,
            10550,
            15550,
            15550,
            25550,
            55550,
            85550,
            115550,
            145550,
            215550,
            315550,
            515550,
            815550,
            1115550,
            1615550,
            2615550,
            4115550,
            6115550,
            9115550,
            13115550,
            20115550,
            30115550,
            45115550,
            61115550,
            91115550,
            105115550,
            105115550,
            120115550,
            135115550,
            150115550,
            165115550,
            180115550,
            195115550,
            210115550,
            225115550,
            240115550,
            255115550,
            270115550,
            285115550,
            300115550,
            315115550,
            330115550,
        )
        private val melonMilestone = listOf(
            150,
            380,
            780,
            1580,
            3880,
            7880,
            15880,
            23880,
            38880,
            78880,
            118880,
            158880,
            208880,
            318880,
            468880,
            768880,
            1168880,
            1568880,
            2368880,
            3868880,
            5868880,
            8868880,
            13868880,
            19868880,
            30868880,
            45868880,
            65868880,
            85868880,
            105868880,
            125868880,
            145868880,
            165868880,
            185868880,
            205868880,
            225868880,
            245868880,
            265868880,
            285868880,
            305868880,
            325868880,
            345868880,
            365868880,
            385868880,
            405868880,
            425868880,
            445868880
        )
        private val pumpkinMilestone = listOf(
            30,
            80,
            160,
            310,
            810,
            1610,
            3110,
            4610,
            7610,
            15610,
            23610,
            31610,
            40610,
            91610,
            151610,
            231610,
            311610,
            461610,
            761610,
            1261610,
            1861610,
            2761610,
            3961610,
            6061610,
            9061610,
            14061610,
            19061610,
            24061610,
            29061610,
            34061610,
            39061610,
            44061610,
            49061610,
            54061610,
            59061610,
            64061610,
            69061610,
            74061610,
            76061610,
            84061610,
            89061610,
            94061610,
            99061610,
            104061610,
            109061610
        )
        private val caneMilestone = listOf(
            60,
            160,
            320,
            660,
            1660,
            3260,
            7260,
            11260,
            17260,
            33260,
            49260,
            65260,
            85260,
            125260,
            185260,
            325260,
            485260,
            645260,
            985260,
            1645260,
            2645260,
            3965260,
            5965260,
            8565260,
            13165260,
            19765260,
            29765260,
            39765260,
            49765260,
            59765260,
            69765260,
            79765260,
            89765260,
            99765260,
            109765260,
            119765260,
            129765260,
            139765260,
            149765260,
            159765260,
            169765260,
            179765260,
            189765260,
            199765260,
            209765260,
            219765260
        )
        private val cocoaMilestone = listOf(
            60,
            160,
            320,
            660,
            1660,
            3260,
            7260,
            11260,
            17260,
            33260,
            49260,
            65260,
            85260,
            125260,
            185260,
            325260,
            485260,
            645260,
            985260,
            1645260,
            2645260,
            3965260,
            5965260,
            8565260,
            13165260,
            19765260,
            29765260,
            39765260,
            49765260,
            59765260,
            69765260,
            79765260,
            89765260,
            99765260,
            109765260,
            119765260,
            129765260,
            139765260,
            149765260,
            159765260,
            169765260,
            179765260,
            189765260,
            199765260,
            209765260,
            219765260
        )
        private val cactusMilestone = listOf(
            60,
            150,
            300,
            600,
            1500,
            3000,
            6000,
            9000,
            15000,
            30000,
            45000,
            60000,
            78000,
            118000,
            178000,
            295000,
            448000,
            598000,
            898000,
            1498000,
            2398000,
            3598000,
            5398000,
            7798000,
            17798000,
            26798000,
            35798000,
            44798000,
            54798000,
            64798000,
            74798000,
            84798000,
            94798000,
            104798000,
            114798000,
            124798000,
            134798000,
            144798000,
            154798000,
            164798000,
            174798000,
            184798000,
            194798000,
            204798000,
            214798000
        )
        private val shroomMilestone = listOf(
            30,
            80,
            160,
            310,
            810,
            1610,
            3110,
            4610,
            7610,
            15610,
            23610,
            31610,
            40610,
            61610,
            91610,
            151610,
            231610,
            311610,
            461610,
            761610,
            1261610,
            1861610,
            2761610,
            3961610,
            6061610,
            9061610,
            14061610,
            19061610,
            24061610,
            29061610,
            34061610,
            39061610,
            44061610,
            49061610,
            54061610,
            59061610,
            64061610,
            69061610,
            74061610,
            79061610,
            84061610,
            89061610,
            94061610,
            99061610,
            104061610,
            109061610
        )
        private val nwMilestone = listOf(
            100,
            250,
            550,
            1050,
            2550,
            5550,
            10550,
            15550,
            25550,
            55550,
            85550,
            115550,
            145550,
            215550,
            315550,
            515550,
            815550,
            1115550,
            1615550,
            2615550,
            4115550,
            6115550,
            9115550,
            13115550,
            20115550,
            30115550,
            45115550,
            60115550,
            75115550,
            90115550,
            105115550,
            120115550,
            135115550,
            150115550,
            165115550,
            180115550,
            195115550,
            210115550,
            225115550,
            240115550,
            255115550,
            270115550,
            284115550,
            300115550,
            315115550,
            330115550
        )
        private val cropMilestoneList = mapOf(
            CropType.WHEAT to wheatMilestone,
            CropType.CARROT to carrotMilestone,
            CropType.POTATO to potatoMilestone,
            CropType.MELON to melonMilestone,
            CropType.PUMPKIN to pumpkinMilestone,
            CropType.SUGAR_CANE to caneMilestone,
            CropType.COCOA_BEANS to cocoaMilestone,
            CropType.CACTUS to cactusMilestone,
            CropType.MUSHROOM to shroomMilestone,
            CropType.NETHER_WART to nwMilestone
        )
    }
}

// TODO delete?
private fun String.formatNumber(): Long {
    var text = replace(",", "")
    val multiplier = if (text.endsWith("k")) {
        text = text.substring(0, text.length - 1)
        1_000
    } else if (text.endsWith("m")) {
        text = text.substring(0, text.length - 1)
        1_000_000
    } else 1
    val d = text.toDouble()
    return (d * multiplier).toLong()
}
