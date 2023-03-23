package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestones.Companion.getCounter
import at.hannibal2.skyhanni.data.SendTitleHelper
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraft.client.audio.ISound
import net.minecraft.client.audio.PositionedSound
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.*

class GardenCropMilestoneDisplay {
    private val progressDisplay = mutableListOf<List<Any>>()
    private val cultivatingData = mutableMapOf<CropType, Int>()
    private val config get() = SkyHanniMod.feature.garden
    private val bestCropTime = GardenBestCropTime()
//    val cropMilestoneLevelUpPattern = Pattern.compile("  §r§b§lGARDEN MILESTONE §3(.*) §8XXIII➜§3(.*)")

    private val sound = object : PositionedSound(ResourceLocation("random.orb")) {
        init {
            volume = 50f
            repeat = false
            repeatDelay = 0
            attenuationType = ISound.AttenuationType.NONE
        }
    }

    private var lastPlaySoundTime = 0L

    private var needsInventory = false

//    @SubscribeEvent
//    fun onChatMessage(event: LorenzChatEvent) {
//        if (!isEnabled()) return
//        if (config.cropMilestoneWarnClose) {
//            val matcher = cropMilestoneLevelUpPattern.matcher(event.message)
//            if (matcher.matches()) {
//                val cropType = matcher.group(1)
//                val newLevel = matcher.group(2).romanToDecimalIfNeeded()
//                LorenzUtils.debug("found milestone messsage!")
//                SendTitleHelper.sendTitle("§b$cropType $newLevel", 1_500)
//            }
//        }
//    }

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
        val item = event.itemStack
        val counter = GardenAPI.readCounter(item)
        if (counter == -1) return
        val crop = GardenAPI.getCropTypeFromItem(item) ?: return
        if (cultivatingData.containsKey(crop)) {
            val old = cultivatingData[crop]!!
            val diff = counter - old
            try {
                GardenCropMilestones.cropCounter[crop] = crop.getCounter() + diff
            } catch (e: NullPointerException) {
                println("crop: '$crop'")
                println("GardenCropMilestones.cropCounter: '${GardenCropMilestones.cropCounter.keys}'")
                LorenzUtils.debug("NPE at OwnInventorItemUpdateEvent with GardenCropMilestones.cropCounter")
                e.printStackTrace()
            }
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
    private var currentCrop: CropType? = null

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
        lastItemInHand = event.toolItem
        currentCrop = event.crop

        if (isEnabled()) {
            resetSpeed()
            update()
        }
    }

    private fun update() {
        progressDisplay.clear()
        bestCropTime.display.clear()
        currentCrop?.let {
            drawProgressDisplay(it, it.getCounter())
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

    private fun drawProgressDisplay(crop: CropType, counter: Long) {
        progressDisplay.add(Collections.singletonList("§6Crop Milestones"))

        val list = mutableListOf<Any>()
        GardenAPI.addGardenCropToList(crop, list)
        list.add(crop.cropName)
        progressDisplay.add(list)

        val currentTier = GardenCropMilestones.getTierForCrops(counter)

        val cropsForCurrentTier = GardenCropMilestones.getCropsForTier(currentTier)
        val nextTier = currentTier + 1
        val cropsForNextTier = GardenCropMilestones.getCropsForTier(nextTier)

        val have = counter - cropsForCurrentTier
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
            GardenAPI.cropsPerSecond[crop] = averageSpeedPerSecond
            val missing = need - have
            val missingTimeSeconds = missing / averageSpeedPerSecond
            val millis = missingTimeSeconds * 1000
            bestCropTime.timeTillNextCrop[crop] = millis
            val duration = TimeUtils.formatDuration(millis)
            if (config.cropMilestoneWarnClose) {
                if (millis < 5_900) {
                    if (System.currentTimeMillis() > lastPlaySoundTime + 1_000) {
                        lastPlaySoundTime = System.currentTimeMillis()
                        sound.playSound()
                    }
                    SendTitleHelper.sendTitle("§b$crop $nextTier in $duration", 1_500)
                }
            }
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