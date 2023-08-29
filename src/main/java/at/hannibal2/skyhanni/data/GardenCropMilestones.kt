package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.CropMilestoneUpdateEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.jsonobjects.GardenJson
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object GardenCropMilestones {
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
                    val amount = group("name").replace(",", "").toLong()
                    crop.setCounter(amount)
                }
            }
        }
        CropMilestoneUpdateEvent().postAndCatch()
    }

    private var cropMilestoneData: Map<CropType, List<Int>>? = null

    val cropCounter: MutableMap<CropType, Long>? get() = GardenAPI.config?.cropCounter

    // TODO make nullable
    fun CropType.getCounter() = cropCounter?.get(this) ?: 0

    fun CropType.setCounter(counter: Long) {
        cropCounter?.set(this, counter)
    }

    fun CropType.isMaxed(): Boolean {
        val maxValue = cropMilestoneData?.get(this)?.sum() ?: 1_000_000_000 // 1 bil for now
        return getCounter() >= maxValue
    }

    fun getTierForCropCount(count: Long, crop: CropType): Int {
        var tier = 0
        var totalCrops = 0L
        val cropMilestone = cropMilestoneData?.get(crop) ?: return 0
        for (tierCrops in cropMilestone) {
            totalCrops += tierCrops
            if (totalCrops > count) {
                return tier
            }
            tier++
        }

        return tier
    }

    fun getMaxTier() = cropMilestoneData?.values?.firstOrNull()?.size ?: 0

    fun getCropsForTier(requestedTier: Int, crop: CropType): Long {
        var totalCrops = 0L
        var tier = 0
        val cropMilestone = cropMilestoneData?.get(crop) ?: return 0
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
        val data = event.getConstant<GardenJson>("Garden") ?: return
        cropMilestoneData = data.crop_milestones
    }
}