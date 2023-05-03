package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CropMilestoneUpdateEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.event.world.WorldEvent
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
    fun onWorldChange(event: WorldEvent.Load) {
        if (cropCounter.isEmpty()) {
            for (crop in CropType.values()) {
                crop.setCounter(0)
            }
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
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
        val cropCounter: MutableMap<CropType, Long> get() = SkyHanniMod.feature.hidden.gardenCropCounter

        fun CropType.getCounter() = cropCounter[this]!!

        fun CropType.setCounter(counter: Long) {
            cropCounter[this] = counter
        }

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
    }
}

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
