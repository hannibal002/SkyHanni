package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestones.Companion.getCounter
import at.hannibal2.skyhanni.data.GardenCropMilestones.Companion.setCounter
import at.hannibal2.skyhanni.data.MayorElection
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.CropType.Companion.getCropType
import at.hannibal2.skyhanni.features.garden.FarmingFortuneDisplay
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenAPI.addCropIcon
import at.hannibal2.skyhanni.features.garden.GardenAPI.getCropType
import at.hannibal2.skyhanni.features.garden.GardenAPI.setSpeed
import at.hannibal2.skyhanni.utils.BlockUtils.isBabyCrop
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.math.abs

class GardenCropMilestoneDisplay {
    private var progressDisplay = listOf<List<Any>>()
    private var mushroomCowPerkDisplay = listOf<List<Any>>()
    private val cultivatingData = mutableMapOf<CropType, Long>()
    private val config get() = SkyHanniMod.feature.garden
    private val bestCropTime = GardenBestCropTime()

    private var lastPlaySoundTime = 0L
    private var needsInventory = false

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!isEnabled()) return
        // TODO remove this once hypixel counts 64x pumpkin drops to cultivating
        if (event.message == "§a§lUNCOMMON DROP! §r§eDicer dropped §r§f64x §r§fPumpkin§r§e!") {
            CropType.PUMPKIN.setCounter(CropType.PUMPKIN.getCounter() + 64)
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return
        if (GardenAPI.hideExtraGuis()) return

        config.cropMilestoneProgressDisplayPos.renderStringsAndItems(
            progressDisplay,
            posLabel = "Crop Milestone Progress"
        )

        if (config.cropMilestoneMushroomPetPerkEnabled) {
            config.cropMilestoneMushroomPetPerkPos.renderStringsAndItems(
                mushroomCowPerkDisplay,
                posLabel = "Mushroom Cow Perk"
            )
        }

        if (config.cropMilestoneBestDisplay) {
            config.cropMilestoneNextDisplayPos.renderStringsAndItems(bestCropTime.display, posLabel = "Best Crop Time")
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
        GardenBestCropTime.updateTimeTillNextCrop()
        update()
    }

    @SubscribeEvent
    fun onOwnInventoryItemUpdate(event: OwnInventorItemUpdateEvent) {
        if (!GardenAPI.inGarden()) return

        try {
            val item = event.itemStack
            val counter = GardenAPI.readCounter(item)
            if (counter == -1L) return
            val crop = item.getCropType() ?: return
            if (cultivatingData.containsKey(crop)) {
                val old = cultivatingData[crop]!!
                val addedCounter = (counter - old).toInt()

                if (GardenCropMilestones.cropCounter.isEmpty()) {
                    for (innerCrop in CropType.values()) {
                        innerCrop.setCounter(0)
                    }
                }
                if (GardenAPI.isSpeedDataEmpty()) {
                    for (cropType in CropType.values()) {
                        cropType.setSpeed(-1)
                    }
                }
                if (!finneganPerkActive()) {
                    crop.setCounter(crop.getCounter() + addedCounter)
                }
                EliteFarmingWeight.addCrop(crop, addedCounter)
                if (currentCrop == crop) {
                    calculateSpeed(addedCounter)
                    update()
                }
            }
            cultivatingData[crop] = counter
        } catch (e: Throwable) {
            LorenzUtils.error("[SkyHanni] Error in OwnInventorItemUpdateEvent")
            e.printStackTrace()
        }
    }

    private fun finneganPerkActive(): Boolean {
        val forcefullyEnabledAlwaysFinnegan = config.forcefullyEnabledAlwaysFinnegan
        val perkActive = MayorElection.isPerkActive("Finnegan", "Farming Simulator")
        MayorElection.currentCandidate?.let {

        }
        return forcefullyEnabledAlwaysFinnegan || perkActive
    }

    @SubscribeEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!isEnabled()) return
        if (event.clickType != ClickType.LEFT_CLICK) return

        val blockState = event.getBlockState

        val cropType = blockState.getCropType() ?: return
        if (cropType.multiplier == 1) {
            if (blockState.isBabyCrop()) return
        }
        blocksBroken++
    }

    private var currentSpeed = 0
    private var averageBlocksPerSecond = 0.0

    private val blocksSpeedList = mutableListOf<Int>()
    private var lastItemInHand: ItemStack? = null
    private var currentCrop: CropType? = null
    private var blocksBroken = 0
    private var lastBlocksBroken = 0
    private var secondsStopped = 0

    private fun resetSpeed() {
        currentSpeed = 0
        averageBlocksPerSecond = 0.0
        blocksBroken = 0
        blocksSpeedList.clear()
        secondsStopped = 0
    }

    init {
        fixedRateTimer(name = "skyhanni-crop-milestone-speed", period = 1000L) {
            if (GardenAPI.inGarden() && GardenAPI.mushroomCowPet) {
                CropType.MUSHROOM.setCounter(CropType.MUSHROOM.getCounter() + blocksBroken)
                update()
            }
            if (isEnabled()) {
                checkSpeed()
            }
        }
    }

    private fun checkSpeed() {
        if (finneganPerkActive()) {
            currentSpeed = (currentSpeed.toDouble() * 0.8).toInt()
        }

        val blocksBroken = blocksBroken.coerceAtMost(20)
        this.blocksBroken = 0

        // skipping seconds when not farming momentarily eg. typing in chat hopefully doesn't skip the time it takes to change between rows
        if (blocksBroken >= config.blocksReset) {
            blocksSpeedList.add(blocksBroken)
            // removing the first second of tracking as it is not a full second
            if (blocksSpeedList.size == 2) {
                blocksSpeedList.removeFirst()
                blocksSpeedList.add(blocksBroken)
            }

            averageBlocksPerSecond = blocksSpeedList.average()
            secondsStopped = 0

        }
        else {
            secondsStopped ++
            if (secondsStopped >= 5) {
                resetSpeed()
            }
        }
        lastBlocksBroken = blocksBroken


        if (finneganPerkActive()) {
            currentCrop?.let {
                it.setCounter(it.getCounter() + currentSpeed)
            }
        }
        currentSpeed = 0
    }


    private fun calculateSpeed(addedCounter: Int) {
        currentSpeed += addedCounter
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
        progressDisplay = emptyList()
        mushroomCowPerkDisplay = emptyList()
        bestCropTime.display = emptyList()
        currentCrop?.let {
            progressDisplay = drawProgressDisplay(it, it.getCounter())
            if (config.cropMilestoneBestDisplay) {
                bestCropTime.display = bestCropTime.drawBestDisplay(it)
            }
        }
        if (config.cropMilestoneBestAlwaysOn) {
            if (currentCrop == null) {
                bestCropTime.display = bestCropTime.drawBestDisplay(null)
            }
        }
    }

    private fun drawProgressDisplay(crop: CropType, counter: Long): MutableList<List<Any>> {
        val lineMap = HashMap<Int, List<Any>>()
        lineMap[0] = Collections.singletonList("§6Crop Milestones")

        val currentTier = GardenCropMilestones.getTierForCrops(counter)
        val nextTier = currentTier + 1

        val list = mutableListOf<Any>()
        list.addCropIcon(crop)
        list.add("§7" + crop.cropName + " Tier $nextTier")
        lineMap[1] = list

        val cropsForCurrentTier = GardenCropMilestones.getCropsForTier(currentTier)
        val cropsForNextTier = GardenCropMilestones.getCropsForTier(nextTier)

        val have = counter - cropsForCurrentTier
        val need = cropsForNextTier - cropsForCurrentTier

        val haveFormat = LorenzUtils.formatInteger(have)
        val needFormat = LorenzUtils.formatInteger(need)
        lineMap[2] = Collections.singletonList("§e$haveFormat§8/§e$needFormat")

        lastItemInHand?.let {
            if (GardenAPI.readCounter(it) == -1L) {
                lineMap[3] = Collections.singletonList("§cWarning: You need Cultivating!")
                return formatDisplay(lineMap)
            }
        }


        val farmingFortune = FarmingFortuneDisplay.getCurrentFarmingFortune(true)
        val farmingFortuneSpeed = (farmingFortune * crop.baseDrops * averageBlocksPerSecond / 100).round(1).toInt()

        if (farmingFortuneSpeed > 0) {
            crop.setSpeed(farmingFortuneSpeed)
            val missing = need - have
            val missingTimeSeconds = missing / farmingFortuneSpeed
            val millis = missingTimeSeconds * 1000
            GardenBestCropTime.timeTillNextCrop[crop] = millis
            val duration = TimeUtils.formatDuration(millis)
            if (config.cropMilestoneWarnClose) {
                if (millis < 5_900) {
                    if (System.currentTimeMillis() > lastPlaySoundTime + 1_000) {
                        lastPlaySoundTime = System.currentTimeMillis()
                        SoundUtils.playBeepSound()
                    }
                    if (!needsInventory) {
                        TitleUtils.sendTitle("§b${crop.cropName} $nextTier in $duration", 1_500)
                    }
                }
            }
            lineMap[3] = Collections.singletonList("§7In §b$duration")

            val format = LorenzUtils.formatInteger(farmingFortuneSpeed * 60)
            lineMap[4] = Collections.singletonList("§7Crops/Minute§8: §e$format")
            val formatBps = LorenzUtils.formatDouble(averageBlocksPerSecond, config.blocksDecimals)
            lineMap[5] = Collections.singletonList("§7Blocks/Second§8: §e$formatBps")
        }

        val percentageFormat = LorenzUtils.formatPercentage(have.toDouble() / need.toDouble())
        lineMap[6] = Collections.singletonList("§7Percentage: §e$percentageFormat")

        if (GardenAPI.mushroomCowPet && crop != CropType.MUSHROOM) {
            addMushroomCowData()
        }

        return formatDisplay(lineMap)
    }

    private fun formatDisplay(lineMap: HashMap<Int, List<Any>>): MutableList<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        for (index in config.cropMilestoneText) {
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

        val currentTier = GardenCropMilestones.getTierForCrops(counter)
        val nextTier = currentTier + 1

        val cropsForCurrentTier = GardenCropMilestones.getCropsForTier(currentTier)
        val cropsForNextTier = GardenCropMilestones.getCropsForTier(nextTier)

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


        if (averageBlocksPerSecond != 0.0) {
            val blocksPerSecond = averageBlocksPerSecond * (currentCrop?.multiplier ?: 1)

            val missingTimeSeconds = missing / blocksPerSecond
            val millis = missingTimeSeconds * 1000
            val duration = TimeUtils.formatDuration(millis.toLong())
            lineMap[3] = Collections.singletonList("§7In §b$duration")
        }

        val percentageFormat = LorenzUtils.formatPercentage(have.toDouble() / need.toDouble())
        lineMap[4] = Collections.singletonList("§7Percentage: §e$percentageFormat")

        val newList = mutableListOf<List<Any>>()
        for (index in config.cropMilestoneMushroomPetPerkText) {
            lineMap[index]?.let {
                newList.add(it)
            }
        }
        mushroomCowPerkDisplay = newList
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.cropMilestoneProgress
}
