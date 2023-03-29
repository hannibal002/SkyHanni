package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestones.Companion.getCounter
import at.hannibal2.skyhanni.data.GardenCropMilestones.Companion.setCounter
import at.hannibal2.skyhanni.data.SendTitleHelper
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.features.garden.GardenAPI.Companion.setSpeed
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraft.block.properties.PropertyInteger
import net.minecraft.block.state.IBlockState
import net.minecraft.client.audio.ISound
import net.minecraft.client.audio.PositionedSound
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.*
import kotlin.concurrent.fixedRateTimer

class GardenCropMilestoneDisplay {
    private var progressDisplay = listOf<List<Any>>()
    private var mushroomCowPerkDisplay = listOf<List<Any>>()
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

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!isEnabled()) return
        // TODO remove this once hypixel counts 64x pumpkin drops to cultivating
        if (event.message == "§a§lUNCOMMON DROP! §r§eDicer dropped §r§f64x §r§fPumpkin§r§e!") {
            CropType.PUMPKIN.setCounter(CropType.PUMPKIN.getCounter() + 64)
        }
//        if (config.cropMilestoneWarnClose) {
//            val matcher = cropMilestoneLevelUpPattern.matcher(event.message)
//            if (matcher.matches()) {
//                val cropType = matcher.group(1)
//                val newLevel = matcher.group(2).romanToDecimalIfNeeded()
//                LorenzUtils.debug("found milestone messsage!")
//                SendTitleHelper.sendTitle("§b$cropType $newLevel", 1_500)
//            }
//        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return

        config.cropMilestoneProgressDisplayPos.renderStringsAndItems(progressDisplay, posLabel = "Crop Milestone Progress")

        if (config.cropMilestoneMushroomPetPerkEnabled) {
            config.cropMilestoneMushroomPetPerkPos.renderStringsAndItems(mushroomCowPerkDisplay, posLabel = "Mushroom Cow Perk")
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
        bestCropTime.updateTimeTillNextCrop()
        update()
    }

    @SubscribeEvent
    fun onOwnInventoryItemUpdate(event: OwnInventorItemUpdateEvent) {
        // TODO remove try catch
        try {
            val item = event.itemStack
            val counter = GardenAPI.readCounter(item)
            if (counter == -1) return
            val crop = GardenAPI.getCropTypeFromItem(item) ?: return
            if (cultivatingData.containsKey(crop)) {
                val old = cultivatingData[crop]!!
                val addedCounter = counter - old

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

                crop.setCounter(crop.getCounter() + addedCounter)
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

    @SubscribeEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!isEnabled()) return

        if (event.clickType == ClickType.LEFT_CLICK) {
            val blockState = event.getBlockState

            if (isFarmBlock(blockState)) {
                for (property in blockState.block.blockState.properties) {
                    val name = property.name
                    if (name == "age") {
                        if (property is PropertyInteger) {
                            val value = blockState.getValue(property)!!
                            if (value == 0) return
                        }
                    }
                }
                blocksBroken++
            }
        }
    }

    private fun isFarmBlock(blockState: IBlockState): Boolean {
        val block = blockState.block
        if (block == Blocks.carrots) return true
        if (block == Blocks.potatoes) return true
        if (block == Blocks.wheat) {
            for (property in block.blockState.properties) {
                val name = property.name
                if (name != "age") continue
                if (property !is PropertyInteger) continue

                val age = blockState.getValue(property)
                if (age == 0) {
                    return false
                }
            }
            return true
        }
        if (block == Blocks.cocoa) return true // Cocoa Beans
        if (block == Blocks.red_mushroom) return true
        if (block == Blocks.brown_mushroom) return true
        if (block == Blocks.cactus) return true
        if (block == Blocks.reeds) return true // Sugar Cane
        if (block == Blocks.nether_wart) return true
        if (block == Blocks.melon_block) return true
        if (block == Blocks.pumpkin) return true

        if (block == Blocks.dirt) return false
        if (block == Blocks.farmland) return false

        return false
    }

    private var currentSpeed = 0
    private var averageSpeedPerSecond = 0
    private var countInLastSecond = 0
    private val allCounters = mutableListOf<Int>()
    private var lastItemInHand: ItemStack? = null
    private var currentCrop: CropType? = null
    private var blocksBroken = 0
    private var lastBlocksPerSecond = 0

    private fun resetSpeed() {
        currentSpeed = 0
        averageSpeedPerSecond = 0
        countInLastSecond = 0
        allCounters.clear()
    }

    init {
        fixedRateTimer(name = "skyhanni-crop-milestone-speed", period = 1000L) {
            if (isEnabled()) {
                checkSpeed()
            }
        }
    }

    private fun checkSpeed() {
        if (countInLastSecond > 8) {
            allCounters.add(currentSpeed)
            while (allCounters.size > 30) {
                allCounters.removeFirst()
            }
            averageSpeedPerSecond = allCounters.average().toInt()
        }
        countInLastSecond = 0
        currentSpeed = 0

        lastBlocksPerSecond = blocksBroken
        val hasMushroomCow = hasMushroomCow()
        if (hasMushroomCow) {
            CropType.MUSHROOM.setCounter(CropType.MUSHROOM.getCounter() + blocksBroken)
        }
        blocksBroken = 0
    }

    private fun hasMushroomCow() = SkyHanniMod.feature.hidden.currentPet.contains("Mooshroom Cow")

    private fun calculateSpeed(addedCounter: Int) {
        currentSpeed += addedCounter
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
        GardenAPI.addGardenCropToList(crop, list)
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
            if (GardenAPI.readCounter(it) == -1) {
                lineMap[3] = Collections.singletonList("§cWarning: You need Cultivating!")
                return formatDisplay(lineMap)
            }
        }

        if (averageSpeedPerSecond != 0) {
            crop.setSpeed(averageSpeedPerSecond)
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
            lineMap[3] = Collections.singletonList("§7In §b$duration")

            val format = LorenzUtils.formatInteger(averageSpeedPerSecond * 60)
            lineMap[4] = Collections.singletonList("§7Crops/Minute§8: §e$format")
            lineMap[5] = Collections.singletonList("§7Blocks/Second§8: §e$lastBlocksPerSecond")


            if (hasMushroomCow() && crop != CropType.MUSHROOM) {
                addMushroomCowData()
            }
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

        // We assume perfect 20 blocks per second speed for this
        val missingTimeSeconds = missing / 20
        val millis = missingTimeSeconds * 1000
        val duration = TimeUtils.formatDuration(millis)

        lineMap[0] = Collections.singletonList("§6Mooshroom Cow Perk")

        val list = mutableListOf<Any>()
        GardenAPI.addGardenCropToList(CropType.MUSHROOM, list)
        list.add("§7Mushroom Tier $nextTier")
        lineMap[1] = list

        lineMap[2] = Collections.singletonList("§e$haveFormat§8/§e$needFormat")
        lineMap[3] = Collections.singletonList("§7In §b$duration")

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