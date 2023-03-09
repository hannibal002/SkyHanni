package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.Garden
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.sorted
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.*

class GardenCropMilestoneDisplay {
    private val progressDisplay = mutableListOf<List<Any>>()
    private val bestCropDisplay = mutableListOf<List<Any>>()
    private var needsInventory = false
    private val cultivatingData = mutableMapOf<String, Int>()
    private val timeTillNextCrop: MutableMap<String, Long> get() = SkyHanniMod.feature.hidden.gardenTimeTillNextCropMilestone
    private val config: Garden get() = SkyHanniMod.feature.garden

    companion object {
        fun getCropsPerSecond(crop: String): Int? {
            if (crop.endsWith(" Mushroom")) {
                return cropsPerSecond["Mushroom"]
            }
            return cropsPerSecond[crop]
        }

       private val cropsPerSecond: MutableMap<String, Int> get() = SkyHanniMod.feature.hidden.gardenCropsPerSecond
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return

        config.cropMilestoneProgressDisplayPos.renderStringsAndItems(progressDisplay)
        if (config.cropMilestoneBestDisplay) {
            config.cropMilestoneNextDisplayPos.renderStringsAndItems(bestCropDisplay)
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onProfileJoin(event: ProfileJoinEvent) {
        if (GardenCropMilestones.cropCounter.values.sum() == 0L) {
            needsInventory = true
        }
        if (cropsPerSecond.isEmpty()) {
            for (key in GardenCropMilestones.cropCounter.keys) {
                cropsPerSecond[key] = -1
            }
        }
    }

    @SubscribeEvent
    fun onCropMilestoneUpdate(event: CropMilestoneUpdateEvent) {
        needsInventory = false
        update()
    }

    @SubscribeEvent
    fun onOwnInventoryItemUpdate(event: OwnInventorItemUpdateEvent) {
        val itemStack = event.itemStack
        val counter = GardenAPI.readCounter(itemStack)
        if (counter == -1) return
        val crop = GardenAPI.getCropTypeFromItem(itemStack) ?: return
        if (cultivatingData.containsKey(crop)) {
            val old = cultivatingData[crop]!!
            val diff = counter - old
            GardenCropMilestones.cropCounter[crop] = GardenCropMilestones.cropCounter[crop]!! + diff
            if (GardenAPI.cropInHand == crop) {
                calculateSpeed(diff)
                update()
            }
        }
        cultivatingData[crop] = counter
    }

    private var lastSecondStart = 0L
    private var currentSpeed = 0
    private var averageSpeedPerSecond = 0
    private var countInLastSecond = 0
    private val allCounters = mutableListOf<Int>()

    private fun resetSpeed() {
        lastSecondStart = 0
        currentSpeed = 0
        averageSpeedPerSecond = 0
        countInLastSecond = 0
        allCounters.clear()
    }

    private fun calculateSpeed(diff: Int) {
        if (System.currentTimeMillis() > lastSecondStart + 1_000) {
            lastSecondStart = System.currentTimeMillis()
            if (countInLastSecond > 8) {
//                println("currentSpeed: $currentSpeed")
                allCounters.add(currentSpeed)
                while (allCounters.size > 30) {
                    allCounters.removeFirst()
                }
//                println("allCounters: $allCounters")
                averageSpeedPerSecond = allCounters.average().toInt()
            }
            countInLastSecond = 0
            currentSpeed = 0
        }
        currentSpeed += diff
        countInLastSecond++
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        if (!isEnabled()) return

        resetSpeed()
        update()
    }

    private fun update() {
        progressDisplay.clear()
        bestCropDisplay.clear()
        GardenAPI.cropInHand?.let {
            val crops = GardenCropMilestones.cropCounter[it]
            if (crops == null) {
                println("cropCounter is null for '$it'")
                return
            }

            drawProgressDisplay(it, crops)
            if (config.cropMilestoneBestDisplay) {
                drawBestDisplay(it)
            }
        }
        if (config.cropMilestoneBestAlwaysOn) {
            if (GardenAPI.cropInHand == null) {
                drawBestDisplay(null)
            }
        }
    }

    private fun drawBestDisplay(currentCrop: String?) {
        val gardenExp = config.cropMilestoneBestType == 0
        val sorted = if (gardenExp) {
            val helpMap = mutableMapOf<String, Long>()
            for ((cropName, time) in timeTillNextCrop) {
                val crops = GardenCropMilestones.cropCounter[cropName]!!
                val currentTier = GardenCropMilestones.getTierForCrops(crops)
                val gardenExpForTier = getGardenExpForTier(currentTier + 1)
                val fakeTime = time / gardenExpForTier
                helpMap[cropName] = fakeTime
            }
            helpMap.sorted()
        } else {
            timeTillNextCrop.sorted()
        }

        val title = if (gardenExp) "§2Garden Experience" else "§bSkyBlock Level"
        bestCropDisplay.add(Collections.singletonList("§eBest Crop Time §7($title§7)"))

        if (sorted.isEmpty()) {
            bestCropDisplay.add(Collections.singletonList("§cFarm crops to add them to this list!"))
        }

        var number = 0
        for (cropName in sorted.keys) {
            val millis = timeTillNextCrop[cropName]!!
            val duration = TimeUtils.formatDuration(millis)

            val isCurrent = cropName == currentCrop
            val color = if (isCurrent) "§e" else ""
            number++
            if (number > config.cropMilestoneShowOnlyBest && !isCurrent) continue
            val cropNameDisplay = "$number# $color$cropName"
            if (gardenExp) {
                val crops = GardenCropMilestones.cropCounter[cropName]!!
                val currentTier = GardenCropMilestones.getTierForCrops(crops)
                val gardenExpForTier = getGardenExpForTier(currentTier + 1)
                bestCropDisplay.add(Collections.singletonList(" $cropNameDisplay §b$duration §7(§2$gardenExpForTier §7Exp)"))
            } else {
                bestCropDisplay.add(Collections.singletonList(" $cropNameDisplay §b$duration"))
            }
        }
    }

    private fun getGardenExpForTier(gardenLevel: Int) = if (gardenLevel > 30) 300 else gardenLevel * 10

    private fun drawProgressDisplay(it: String, crops: Long) {
        progressDisplay.add(Collections.singletonList("§6Crop Milestones"))

        val list = mutableListOf<Any>()

        try {
            val internalName = NEUItems.getInternalName(if (it == "Mushroom") "Red Mushroom" else it)
            val itemStack = NEUItems.getItemStack(internalName)
            list.add(itemStack)
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
        list.add(it)
        progressDisplay.add(list)

        val currentTier = GardenCropMilestones.getTierForCrops(crops)

        val cropsForCurrentTier = GardenCropMilestones.getCropsForTier(currentTier)
        val nextTier = currentTier + 1
        val cropsForNextTier = GardenCropMilestones.getCropsForTier(nextTier)

        val have = crops - cropsForCurrentTier
        val need = cropsForNextTier - cropsForCurrentTier

        val haveFormat = LorenzUtils.formatInteger(have)
        val needFormat = LorenzUtils.formatInteger(need)
        progressDisplay.add(Collections.singletonList("§7Progress to Tier $nextTier§8:"))
        progressDisplay.add(Collections.singletonList("§e$haveFormat§8/§e$needFormat"))

        if (averageSpeedPerSecond != 0) {
            cropsPerSecond[it] = averageSpeedPerSecond
            val missing = need - have
            val missingTimeSeconds = missing / averageSpeedPerSecond
            val millis = missingTimeSeconds * 1000
            timeTillNextCrop[it] = millis
            val duration = TimeUtils.formatDuration(millis)
            progressDisplay.add(Collections.singletonList("§7in §b$duration"))

            val format = LorenzUtils.formatInteger(averageSpeedPerSecond * 60)
            progressDisplay.add(Collections.singletonList("§7Crops/minute§8: §e$format"))
        }

        if (needsInventory) {
            progressDisplay.add(Collections.singletonList("§cOpen §e/cropmilestones §cto update!"))
        }
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.cropMilestoneProgress
}