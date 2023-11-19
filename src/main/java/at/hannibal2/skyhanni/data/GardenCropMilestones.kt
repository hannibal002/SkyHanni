package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CropMilestoneUpdateEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.nextAfter
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.jsonobjects.GardenJson
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object GardenCropMilestones {
    // TODO USE SH-REPO
    private val cropPattern = "§7Harvest §f(?<name>.*) §7on .*".toPattern()
    private val totalPattern = "§7Total: §a(?<name>.*)".toPattern()

    fun getCropTypeByLore(itemStack: ItemStack): CropType? {
        for (line in itemStack.getLore()) {
            cropPattern.matchMatcher(line) {
                val name = group("name")
                return CropType.getByNameOrNull(name)
            }
        }
        return null
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Crop Milestones") return

        for ((_, stack) in event.inventoryItems) {
            val crop = getCropTypeByLore(stack) ?: continue
            for (line in stack.getLore()) {
                totalPattern.matchMatcher(line) {
                    val amount = group("name").formatNumber()
                    crop.setCounter(amount)
                }
            }
        }
        CropMilestoneUpdateEvent().postAndCatch()
        if (SkyHanniMod.feature.garden.copyMilestoneData) {
            fixForWrongData(event.inventoryItems)
        }
    }

    private fun fixForWrongData(inventoryItems: Map<Int, ItemStack>) {
        val data = mutableListOf<String>()
        for ((_, stack) in inventoryItems) {
            val crop = getCropTypeByLore(stack) ?: continue
            checkForWrongData(stack, crop, data)

            CropMilestoneUpdateEvent().postAndCatch()
        }

        if (data.isNotEmpty()) {
            LorenzUtils.chat("Found §c${data.size} §ewrong crop milestone steps in the menu! Correct data got put into clipboard. Please share it on SkyHanni Discord.")
            OSUtils.copyToClipboard("```${data.joinToString("\n")}```")
        }
    }

    private fun checkForWrongData(
        stack: ItemStack,
        crop: CropType,
        wrongData: MutableList<String>
    ) {
        val pattern = ".*§e(?<having>.*)§6/§e(?<max>.*)".toPattern()
        val name = stack.name ?: return
        val rawNumber = name.removeColor().replace(crop.cropName, "").trim()
        val realTier = if (rawNumber == "") 0 else rawNumber.romanToDecimalIfNeeded()

        val lore = stack.getLore()
        val next = lore.nextAfter({ totalPattern.matches(it) }, 3) ?: return
        val total = lore.nextAfter({ totalPattern.matches(it) }, 6) ?: return

        debug(" ")
        debug("crop: $crop")
        debug("realTier: $realTier")

        val guessNextMax = getCropsForTier(realTier + 1, crop) - getCropsForTier(realTier, crop)
        debug("guessNextMax: ${guessNextMax.addSeparators()}")
        val nextMax = pattern.matchMatcher(next) {
            group("max").formatNumber()
        } ?: return
        debug("nextMax real: ${nextMax.addSeparators()}")
        if (nextMax != guessNextMax) {
            debug("wrong, add to list")
            wrongData.add("$crop:$realTier:${nextMax.addSeparators()}")
        }

        val guessTotalMax = getCropsForTier(46, crop)
        //             println("guessTotalMax: ${guessTotalMax.addSeparators()}")
        val totalMax = pattern.matchMatcher(total) {
            group("max").formatNumber()
        } ?: return
        //             println("totalMax real: ${totalMax.addSeparators()}")
        val totalOffBy = guessTotalMax - totalMax
        debug("totalOffBy: $totalOffBy")
    }

    fun debug(message: String) {
        if (SkyHanniMod.feature.dev.debug.enabled) {
            println(message)
        }
    }

    private var cropMilestoneData: Map<CropType, List<Int>> = emptyMap()

    val cropCounter: MutableMap<CropType, Long>? get() = GardenAPI.storage?.cropCounter

    // TODO make nullable
    fun CropType.getCounter() = cropCounter?.get(this) ?: 0

    fun CropType.setCounter(counter: Long) {
        cropCounter?.set(this, counter)
    }

    fun CropType.isMaxed(): Boolean {
        // TODO change 1b
        val maxValue = cropMilestoneData[this]?.sum() ?: 1_000_000_000 // 1 bil for now
        return getCounter() >= maxValue
    }

    fun getTierForCropCount(count: Long, crop: CropType): Int {
        var tier = 0
        var totalCrops = 0L
        val cropMilestone = cropMilestoneData[crop] ?: return 0
        for (tierCrops in cropMilestone) {
            totalCrops += tierCrops
            if (totalCrops > count) {
                return tier
            }
            tier++
        }

        return tier
    }

    fun getMaxTier() = cropMilestoneData.values.firstOrNull()?.size ?: 0

    fun getCropsForTier(requestedTier: Int, crop: CropType): Long {
        var totalCrops = 0L
        var tier = 0
        val cropMilestone = cropMilestoneData[crop] ?: return 0
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
        val startTier = getTierForCropCount(progress, this)
        val startCrops = getCropsForTier(startTier, this)
        val end = getCropsForTier(startTier + 1, this).toDouble()
        return (progress - startCrops) / (end - startCrops)
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        cropMilestoneData = event.getConstant<GardenJson>("Garden").crop_milestones
    }
}
