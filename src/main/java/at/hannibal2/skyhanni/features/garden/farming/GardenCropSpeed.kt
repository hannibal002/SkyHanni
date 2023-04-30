package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.GardenCropMilestones.Companion.getCounter
import at.hannibal2.skyhanni.data.GardenCropMilestones.Companion.setCounter
import at.hannibal2.skyhanni.data.MayorElection
import at.hannibal2.skyhanni.events.CropClickEvent
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.concurrent.fixedRateTimer
import kotlin.math.abs

object GardenCropSpeed {
    private val config get() = SkyHanniMod.feature.garden

    var lastBrokenCrop: CropType? = null
    var averageBlocksPerSecond = 0.0

    private val blocksSpeedList = mutableListOf<Int>()
    private var blocksBroken = 0
    private var lastBlocksBroken = 0


    init {
        fixedRateTimer(name = "skyhanni-crop-milestone-speed", period = 1000L) {
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
        val blocksBroken = blocksBroken.coerceAtMost(20)
        this.blocksBroken = 0

        // If the difference in speed between the current and the previous bps value is too high, disregard this value
        if (abs(lastBlocksBroken - blocksBroken) > 4) {
            if (blocksSpeedList.isNotEmpty()) {
                blocksSpeedList.removeLast()
            }
        } else if (blocksBroken >= 2) {
            blocksSpeedList.add(blocksBroken)
            while (blocksSpeedList.size > 120) {
                blocksSpeedList.removeFirst()
            }
            averageBlocksPerSecond = blocksSpeedList.average()

        }
        lastBlocksBroken = blocksBroken
    }

    private fun resetSpeed() {
        averageBlocksPerSecond = 0.0
        blocksBroken = 0
        blocksSpeedList.clear()
    }

    fun finneganPerkActive(): Boolean {
        val forcefullyEnabledAlwaysFinnegan = config.forcefullyEnabledAlwaysFinnegan
        val perkActive = MayorElection.isPerkActive("Finnegan", "Farming Simulator")
        return forcefullyEnabledAlwaysFinnegan || perkActive
    }

    fun isEnabled() = GardenAPI.inGarden()
}