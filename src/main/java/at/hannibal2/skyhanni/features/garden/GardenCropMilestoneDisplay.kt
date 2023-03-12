package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.*

class GardenCropMilestoneDisplay {
    private val progressDisplay = mutableListOf<List<Any>>()
    private val cultivatingData = mutableMapOf<String, Int>()
    private val config get() = SkyHanniMod.feature.garden
    private val bestCropTime = GardenBestCropTime()

    private var needsInventory = false

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return

        config.cropMilestoneProgressDisplayPos.renderStringsAndItems(progressDisplay)
        if (config.cropMilestoneBestDisplay) {
            config.cropMilestoneNextDisplayPos.renderStringsAndItems(bestCropTime.display)
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onProfileJoin(event: ProfileJoinEvent) {
        if (GardenCropMilestones.cropCounter.values.sum() == 0L) {
            needsInventory = true
        }
    }

    @SubscribeEvent
    fun onCropMilestoneUpdate(event: CropMilestoneUpdateEvent) {
        needsInventory = false
        bestCropTime.updateTimeTillNextCrop()
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
            EliteFarmingWeight.addCrop(crop, diff)
            if (currentCrop == crop) {
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
    private var lastItemInHand: ItemStack? = null
    private var currentCrop: String? = null

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
                allCounters.add(currentSpeed)
                while (allCounters.size > 30) {
                    allCounters.removeFirst()
                }
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
        lastItemInHand = if (event.isRealCrop) event.heldItem else null
        currentCrop = if (event.isRealCrop) event.crop else null

        if (isEnabled()) {
            resetSpeed()
            update()
        }
    }

    private fun update() {
        progressDisplay.clear()
        bestCropTime.display.clear()
        currentCrop?.let {
            val crops = GardenCropMilestones.cropCounter[it]
            if (crops == null) {
                println("cropCounter is null for '$it'")
                return
            }

            drawProgressDisplay(it, crops)
            if (config.cropMilestoneBestDisplay) {
                bestCropTime.drawBestDisplay(it)
            }
        }
        if (config.cropMilestoneBestAlwaysOn) {
            if (currentCrop == null) {
                bestCropTime.drawBestDisplay(null)
            }
        }
    }

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

        lastItemInHand?.let {
            if (GardenAPI.readCounter(it) == -1) {
                progressDisplay.add(Collections.singletonList("§cWarning: You need Cultivating!"))
                return
            }
        }

        if (averageSpeedPerSecond != 0) {
            GardenAPI.cropsPerSecond[it] = averageSpeedPerSecond
            val missing = need - have
            val missingTimeSeconds = missing / averageSpeedPerSecond
            val millis = missingTimeSeconds * 1000
            bestCropTime.timeTillNextCrop[it] = millis
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