package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.CropMilestoneUpdateEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class GardenCropMilestones {
    private val overflowPattern = Pattern.compile("(?:.*) §e(.*)§6\\/(?:.*)")
    private val nextTierPattern = Pattern.compile("§7Progress to Tier (.*): §e(?:.*)")

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
    fun onProfileJoin(event: ProfileJoinEvent) {
        cropCounter.clear()

        cropCounter["Wheat"] = 0
        cropCounter["Carrot"] = 0
        cropCounter["Potato"] = 0
        cropCounter["Pumpkin"] = 0
        cropCounter["Sugar Cane"] = 0
        cropCounter["Melon"] = 0
        cropCounter["Cactus"] = 0
        cropCounter["Cocoa Beans"] = 0
        cropCounter["Mushroom"] = 0
        cropCounter["Nether Wart"] = 0
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (event.inventoryName != "Crop Milestones") return

        for ((_, stack) in event.inventoryItems) {
            val cropName = stack.name?.removeColor() ?: continue

            val lore = stack.getLore()
            var cropForTier = 0L
            var next = false
            for (line in lore) {
                if (line.contains("Progress to Tier")) {
                    val matcher = nextTierPattern.matcher(line)
                    if (matcher.matches()) {
                        val nextTier = matcher.group(1).romanToDecimal()
                        val currentTier = nextTier - 1
                        cropForTier = getCropsForTier(currentTier)
                    }
                    next = true
                    continue
                }
                if (next) {
                    val matcher = overflowPattern.matcher(line)
                    if (matcher.matches()) {
                        val rawNumber = matcher.group(1)
                        val overflow = rawNumber.formatNumber()
                        cropCounter[cropName] = cropForTier + overflow
                    }
                    next = false
                }
            }
        }

        CropMilestoneUpdateEvent().postAndCatch()
    }

    companion object {
        val cropCounter = mutableMapOf<String, Long>()

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
