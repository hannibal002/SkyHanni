package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.garden.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.garden.farming.CropClickEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.editCopy
import kotlin.concurrent.fixedRateTimer

@SkyHanniModule
object GardenCropSpeed {

    private val config get() = GardenApi.config
    private val cropsPerSecond: MutableMap<CropType, Int>? get() = GardenApi.storage?.cropsPerSecond
    private val latestBlocksPerSecond: MutableMap<CropType, Double>? get() = GardenApi.storage?.latestBlocksPerSecond

    var lastBrokenCrop: CropType? = null
    var lastBrokenTime = SimpleTimeMark.now()

    var averageBlocksPerSecond = 0.0

    private var blocksSpeedList = listOf<Int>()
    private var blocksBroken = 0
    private var secondsStopped = 0

    init {
        // TODO use SecondPassedEvent + passedSince
        fixedRateTimer(name = "skyhanni-crop-milestone-speed", period = 1000L) {
            if (isEnabled()) {
                checkSpeed()
            }
        }
    }

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        lastBrokenCrop = null
    }

    @HandleEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        if (isEnabled()) {
            resetSpeed()
            update()
        }
    }

    private fun update() {
        GardenCropMilestoneDisplay.update()
    }

    @HandleEvent
    fun onCropClick(event: CropClickEvent) {
        if (event.clickType != ClickType.LEFT_CLICK) return

        lastBrokenCrop = event.crop
        lastBrokenTime = SimpleTimeMark.now()
        blocksBroken++
    }

    private fun checkSpeed() {
        val blocksBroken = blocksBroken
        this.blocksBroken = 0

        if (blocksBroken == 0) {
            if (blocksSpeedList.isEmpty()) return
            secondsStopped++
        } else {
            if (secondsStopped >= config.cropMilestones.blocksBrokenResetTime) {
                resetSpeed()
            }
            blocksSpeedList = blocksSpeedList.editCopy {
                while (secondsStopped > 0) {
                    this.add(0)
                    secondsStopped -= 1
                }
                this.add(blocksBroken)
                if (this.size == 2) {
                    this.removeFirst()
                    this.add(blocksBroken)
                }
            }
            averageBlocksPerSecond = if (blocksSpeedList.size > 5) {
                blocksSpeedList.drop(3).average().coerceAtMost(20.0)
            } else if (blocksSpeedList.size > 1) {
                blocksSpeedList.drop(1).average().coerceAtMost(20.0)
            } else 0.0
            GardenApi.getCurrentlyFarmedCrop()?.let {
                if (averageBlocksPerSecond > 1) {
                    latestBlocksPerSecond?.put(it, averageBlocksPerSecond)
                }
            }
        }
    }

    fun getRecentBPS(): Double {
        val size = blocksSpeedList.size
        return if (size <= 1) {
            0.0
        } else {
            val startIndex = if (size >= 6) size - 6 else 0
            val validValues = blocksSpeedList.subList(startIndex, size)
            validValues.dropLast(1).average()
        }
    }

    private fun resetSpeed() {
        averageBlocksPerSecond = 0.0
        blocksSpeedList = emptyList()
        secondsStopped = 0
    }

    fun isEnabled() = GardenApi.inGarden()

    fun CropType.getSpeed() = cropsPerSecond?.get(this)

    fun CropType.setSpeed(speed: Int) {
        cropsPerSecond?.put(this, speed)
    }

    fun CropType.getLatestBlocksPerSecond() = latestBlocksPerSecond?.get(this)

    fun isSpeedDataEmpty() = cropsPerSecond?.values?.sum()?.let { it == 0 } ?: true

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.blocksBrokenResetTime", "garden.cropMilestones.blocksBrokenResetTime")
    }
}
