package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestones.getCounter
import at.hannibal2.skyhanni.data.GardenCropMilestones.isMaxed
import at.hannibal2.skyhanni.data.GardenCropMilestones.setCounter
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.CropMilestoneUpdateEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.FarmingFortuneDisplay
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenAPI.addCropIcon
import at.hannibal2.skyhanni.features.garden.GardenAPI.getCropType
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.setSpeed
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.Collections
import kotlin.time.Duration.Companion.seconds

object GardenCropMilestoneDisplay {
    private var progressDisplay = emptyList<List<Any>>()
    private var mushroomCowPerkDisplay = emptyList<List<Any>>()
    private val cultivatingData = mutableMapOf<CropType, Long>()
    private val config get() = SkyHanniMod.feature.garden.cropMilestones
    private val bestCropTime = GardenBestCropTime()

    private var lastPlaySoundTime = 0L
    private var needsInventory = false

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        LorenzUtils.onToggle(
            config.bestShowMaxedNeeded,
            config.highestTimeFormat,
        ) {
            GardenBestCropTime.updateTimeTillNextCrop()
            update()
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return
        if (GardenAPI.hideExtraGuis()) return

        config.progressDisplayPos.renderStringsAndItems(
            progressDisplay, posLabel = "Crop Milestone Progress"
        )

        if (config.mushroomPetPerk.enabled) {
            config.mushroomPetPerk.pos.renderStringsAndItems(
                mushroomCowPerkDisplay, posLabel = "Mushroom Cow Perk"
            )
        }

        if (config.next.bestDisplay) {
            config.next.displayPos.renderStringsAndItems(bestCropTime.display, posLabel = "Best Crop Time")
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onProfileJoin(event: ProfileJoinEvent) {
        GardenCropMilestones.cropCounter?.let {
            if (it.values.sum() == 0L) {
                needsInventory = true
            }
        }
    }

    @SubscribeEvent
    fun onCropMilestoneUpdate(event: CropMilestoneUpdateEvent) {
        needsInventory = false
        GardenBestCropTime.updateTimeTillNextCrop()
        update()
    }

    @SubscribeEvent
    fun onOwnInventoryItemUpdate(event: OwnInventoryItemUpdateEvent) {
        if (!GardenAPI.inGarden()) return

        try {
            val item = event.itemStack
            val counter = GardenAPI.readCounter(item)
            if (counter == -1L) return
            val crop = item.getCropType() ?: return
            if (cultivatingData.containsKey(crop)) {
                val old = cultivatingData[crop]!!
                val addedCounter = (counter - old).toInt()
                FarmingWeightDisplay.addCrop(crop, addedCounter)
                update()
                crop.setCounter(
                    crop.getCounter() + if (GardenCropSpeed.finneganPerkActive()) {
                        (addedCounter.toDouble() * 0.8).toInt()
                    } else addedCounter
                )
            }
            cultivatingData[crop] = counter
        } catch (e: Throwable) {
            LorenzUtils.error("[SkyHanni] Error in OwnInventoryItemUpdateEvent")
            e.printStackTrace()
        }
    }

    fun update() {
        progressDisplay = emptyList()
        mushroomCowPerkDisplay = emptyList()
        bestCropTime.display = emptyList()
        val currentCrop = GardenAPI.getCurrentlyFarmedCrop()
        currentCrop?.let {
            progressDisplay = drawProgressDisplay(it)
        }

        if (config.next.bestDisplay && config.next.bestAlwaysOn || currentCrop != null) {
            bestCropTime.display = bestCropTime.drawBestDisplay(currentCrop)
        }
    }

    private fun drawProgressDisplay(crop: CropType): MutableList<List<Any>> {
        val counter = crop.getCounter()
        val lineMap = HashMap<Int, List<Any>>()
        lineMap[0] = Collections.singletonList("§6Crop Milestones")

        val currentTier = GardenCropMilestones.getTierForCropCount(counter, crop)
        val nextTier = if (config.bestShowMaxedNeeded.get()) 46 else currentTier + 1

        val list = mutableListOf<Any>()
        list.addCropIcon(crop)
        if (crop.isMaxed()) {
            list.add("§7" + crop.cropName + " §eMAXED")
        } else {
            list.add("§7" + crop.cropName + " $currentTier➜$nextTier")
        }
        lineMap[1] = list

        val cropsForNextTier = GardenCropMilestones.getCropsForTier(nextTier, crop)
        val (have, need) = if (config.bestShowMaxedNeeded.get()) {
            Pair(counter, cropsForNextTier)
        } else {
            val cropsForCurrentTier = GardenCropMilestones.getCropsForTier(currentTier, crop)
            val have = counter - cropsForCurrentTier
            val need = cropsForNextTier - cropsForCurrentTier
            Pair(have, need)
        }

        lineMap[2] = if (crop.isMaxed()) {
            val haveFormat = LorenzUtils.formatInteger(counter)
            Collections.singletonList("§7Counter: §e$haveFormat")
        } else {
            val haveFormat = LorenzUtils.formatInteger(have)
            val needFormat = LorenzUtils.formatInteger(need)
            Collections.singletonList("§e$haveFormat§8/§e$needFormat")
        }

        val farmingFortune = FarmingFortuneDisplay.getCurrentFarmingFortune(true)
        val speed = GardenCropSpeed.averageBlocksPerSecond
        val farmingFortuneSpeed = (farmingFortune * crop.baseDrops * speed / 100).round(1).toInt()

        if (farmingFortuneSpeed > 0) {
            crop.setSpeed(farmingFortuneSpeed)
            if (crop.isMaxed()) {
                lineMap[3] = listOf("§7In §bMaxed")
            } else {
                val missing = need - have
                val missingTimeSeconds = missing / farmingFortuneSpeed
                val millis = missingTimeSeconds * 1000
                GardenBestCropTime.timeTillNextCrop[crop] = millis
                val biggestUnit = TimeUnit.entries[config.highestTimeFormat.get()]
                val duration = TimeUtils.formatDuration(millis, biggestUnit)
                tryWarn(millis, "§b${crop.cropName} $nextTier in $duration")
                val speedText = "§7In §b$duration"
                lineMap[3] = Collections.singletonList(speedText)
                GardenAPI.itemInHand?.let {
                    if (GardenAPI.readCounter(it) == -1L) {
                        lineMap[3] = listOf(speedText, " §7Inaccurate!")
                    }
                }
            }

            val format = LorenzUtils.formatInteger(farmingFortuneSpeed * 60)
            lineMap[4] = Collections.singletonList("§7Crops/Minute§8: §e$format")
            val formatBps = LorenzUtils.formatDouble(speed, config.blocksBrokenPrecision)
            lineMap[5] = Collections.singletonList("§7Blocks/Second§8: §e$formatBps")
        }

        val percentageFormat = LorenzUtils.formatPercentage(have.toDouble() / need.toDouble())
        lineMap[6] = if (crop.isMaxed()) {
            Collections.singletonList("§7Percentage: §e100%")
        } else {
            Collections.singletonList("§7Percentage: §e$percentageFormat")
        }

        if (GardenAPI.mushroomCowPet && crop != CropType.MUSHROOM) {
            addMushroomCowData()
        }

        return formatDisplay(lineMap)
    }

    private fun tryWarn(millis: Long, title: String) {
        if (!config.warnClose) return
        if (GardenCropSpeed.lastBrokenTime + 500 <= System.currentTimeMillis()) return
        if (millis > 5_900) return

        if (System.currentTimeMillis() > lastPlaySoundTime + 1_000) {
            lastPlaySoundTime = System.currentTimeMillis()
            SoundUtils.playBeepSound()
        }
        if (!needsInventory) {
            TitleUtils.sendTitle(title, 1.5.seconds)
        }
    }

    private fun formatDisplay(lineMap: HashMap<Int, List<Any>>): MutableList<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        for (index in config.text) {
            lineMap[index]?.let {
                newList.add(it)
            }
        }

        if (needsInventory) {
            newList.addAsSingletonList("§cOpen §e/cropmilestones §cto update!")
        }

        return newList
    }

    private fun addMushroomCowData() {
        val lineMap = HashMap<Int, List<Any>>()
        val counter = CropType.MUSHROOM.getCounter()

        val currentTier = GardenCropMilestones.getTierForCropCount(counter, CropType.MUSHROOM)
        val nextTier = currentTier + 1

        val cropsForCurrentTier = GardenCropMilestones.getCropsForTier(currentTier, CropType.MUSHROOM)
        val cropsForNextTier = GardenCropMilestones.getCropsForTier(nextTier, CropType.MUSHROOM)

        val have = counter - cropsForCurrentTier
        val need = cropsForNextTier - cropsForCurrentTier

        val haveFormat = LorenzUtils.formatInteger(have)
        val needFormat = LorenzUtils.formatInteger(need)

        val missing = need - have

        lineMap[0] = Collections.singletonList("§6Mooshroom Cow Perk")

        val list = mutableListOf<Any>()
        list.addCropIcon(CropType.MUSHROOM)
        list.add("§7Mushroom Tier $nextTier")
        lineMap[1] = list

        lineMap[2] = Collections.singletonList("§e$haveFormat§8/§e$needFormat")

        val speed = GardenCropSpeed.averageBlocksPerSecond
        if (speed != 0.0) {
            val blocksPerSecond = speed * (GardenAPI.getCurrentlyFarmedCrop()?.multiplier ?: 1)

            val missingTimeSeconds = missing / blocksPerSecond
            val millis = missingTimeSeconds * 1000
            val biggestUnit = TimeUnit.entries[config.highestTimeFormat.get()]
            val duration = TimeUtils.formatDuration(millis.toLong(), biggestUnit)
            lineMap[3] = Collections.singletonList("§7In §b$duration")
        }

        val percentageFormat = LorenzUtils.formatPercentage(have.toDouble() / need.toDouble())
        lineMap[4] = Collections.singletonList("§7Percentage: §e$percentageFormat")

        val newList = mutableListOf<List<Any>>()
        for (index in config.mushroomPetPerk.text) {
            lineMap[index]?.let {
                newList.add(it)
            }
        }
        mushroomCowPerkDisplay = newList
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.progress

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent){
        event.move(3,"garden.cropMilestoneProgress", "garden.cropMilestones.progress")
        event.move(3, "garden.cropMilestoneWarnClose", "garden.cropMilestones.warnClose")
        event.move(3, "garden.cropMilestoneHighestTimeFormat","garden.cropMilestones.highestTimeFormat")
        event.move(3,"garden.cropMilestoneBestShowMaxedNeeded","garden.cropMilestones.bestShowMaxedNeeded")
        event.move(3,"garden.cropMilestoneText","garden.cropMilestones.text")
        event.move(3,"garden.blocksBrokenPrecision", "garden.cropMilestones.blocksBrokenPrecision")
        event.move(3, "garden.cropMilestoneProgressDisplayPos", "garden.cropMilestones.progressDisplayPos")
        event.move(3, "garden.cropMilestoneBestDisplay", "garden.cropMilestones.next.bestDisplay")
        event.move(3, "garden.cropMilestoneBestAlwaysOn", "garden.cropMilestones.next.bestAlwaysOn")
        event.move(3, "garden.cropMilestoneNextDisplayPos", "garden.cropMilestones.next.displayPos")
        event.move(3,"garden.cropMilestoneMushroomPetPerkEnabled","garden.cropMilestones.mushroomPetPerk.enabled")
        event.move(3,"garden.cropMilestoneMushroomPetPerkText","garden.cropMilestones.mushroomPetPerk.text")
        event.move(3,"garden.cropMilestoneMushroomPetPerkPos","garden.cropMilestones.mushroomPetPerk.pos")
    }
}
