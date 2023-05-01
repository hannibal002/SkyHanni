package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.GardenCropMilestones.Companion.getCounter
import at.hannibal2.skyhanni.data.GardenCropMilestones.Companion.setCounter
import at.hannibal2.skyhanni.data.MayorElection
import at.hannibal2.skyhanni.events.CropClickEvent
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.concurrent.fixedRateTimer

object GardenCropSpeed {
    private val config get() = SkyHanniMod.feature.garden
    private val hidden get() = SkyHanniMod.feature.hidden
    private val cropsPerSecond: MutableMap<CropType, Int> get() = hidden.gardenCropsPerSecond
    private val latestBlocksPerSecond: MutableMap<CropType, Double> get() = hidden.gardenLatestBlocksPerSecond
    private const val updatesPerSecond: Int = 20

    var lastBrokenCrop: CropType? = null
    var averageBlocksPerSecond = 0.0

    private val blocksSpeedList = mutableListOf<Int>()
    private var blocksBroken = 0
    private var secondsStopped = 0


    init {
        fixedRateTimer(name = "skyhanni-crop-milestone-speed", period = 1000L/updatesPerSecond) {
            if (isEnabled()) {
                if (GardenAPI.mushroomCowPet) {
                    CropType.MUSHROOM.setCounter(CropType.MUSHROOM.getCounter() + blocksBroken)
                }
                checkSpeed()
                update()
            }
        }
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        if (isEnabled()) {
            resetSpeed()
            update()
        }
    }

    private fun update() {
        GardenCropMilestoneDisplay.update()
    }

    @SubscribeEvent
    fun onBlockClick(event: CropClickEvent) {
        if (!GardenAPI.inGarden()) return
        if (event.clickType != ClickType.LEFT_CLICK) return

        lastBrokenCrop = event.crop
        blocksBroken++
    }

    private fun checkSpeed() {
        val blocksBroken = blocksBroken
        this.blocksBroken = 0

        if (blocksBroken == 0) {
            if (blocksSpeedList.size == 0) return
            secondsStopped++
        } else {
            if (secondsStopped >= config.blocksBrokenResetTime * updatesPerSecond) {
                resetSpeed()
            }
            while (secondsStopped > 0) {
                blocksSpeedList.add(0)
                secondsStopped -= 1
            }
            blocksSpeedList.add(blocksBroken)
            averageBlocksPerSecond = blocksSpeedList.average() * updatesPerSecond

            GardenAPI.getCurrentlyFarmedCrop()?.let {
                latestBlocksPerSecond[it] = averageBlocksPerSecond
            }
        }
    }

    private fun resetSpeed() {
        averageBlocksPerSecond = 0.0
        blocksSpeedList.clear()
        secondsStopped = 0
    }

    fun finneganPerkActive(): Boolean {
        val forcefullyEnabledAlwaysFinnegan = config.forcefullyEnabledAlwaysFinnegan
        val perkActive = MayorElection.isPerkActive("Finnegan", "Farming Simulator")
        return forcefullyEnabledAlwaysFinnegan || perkActive
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onProfileJoin(event: ProfileJoinEvent) {
        if (cropsPerSecond.isEmpty()) {
            for (cropType in CropType.values()) {
                cropType.setSpeed(-1)
            }
        }
    }

    fun isEnabled() = GardenAPI.inGarden()

    fun CropType.getSpeed(): Int {
        val speed = cropsPerSecond[this]
        if (speed != null) return speed

        val message = "Set speed for $this to -1!"
        println(message)
        LorenzUtils.debug(message)
        setSpeed(-1)
        return -1
    }

    fun CropType.setSpeed(speed: Int) {
        cropsPerSecond[this] = speed
    }

    fun CropType.getLatestBlocksPerSecond() = latestBlocksPerSecond[this]

    fun isSpeedDataEmpty() = cropsPerSecond.values.sum() < 0
}