package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.plots
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.sendTitle
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

class LaneswitchNotification {

    private val config get() = GardenAPI.config.laneswitch

    private var blocksPerSecond = 0.0
    private var lastBlocksPerSecond = 0.0
    private var lastPos = LorenzVec(0, 0, 0)
    private var lastLaneSwitch = SimpleTimeMark.farPast()

    private fun switchPossibleInTime(from: LorenzVec, to: LorenzVec, speed: Double, time: Int): Boolean {
        return from.distance(to) <= speed * time
    }

    private fun playUserSound() {
        SoundUtils.createSound(
            config.notification.sound.notificationSound,
            config.notification.sound.notificationPitch,
        ).playSound()
    }

    private fun getFarmBounds(plotIndex: Int, playerPos: LorenzVec, lastPos: LorenzVec): List<LorenzVec> {
        val xVelocity = playerPos.x - lastPos.x
        val zVelocity = playerPos.z - lastPos.z
        if (xVelocity.absoluteValue > zVelocity.absoluteValue) {
            val xValueMin =
                if (plotIndex - 1 == -1 || !plots[plotIndex - 1].owned || plots.indexOf(plots[plotIndex - 1]) == 12 )
                    plots[plotIndex].box.minX else plots[plotIndex - plotIndex % 5].box.minX
            val xValueMax =
                if ((plotIndex + 1) % 5 == 0 || !plots[plotIndex + 1].owned || plots.indexOf(plots[plotIndex + 1]) == 12)
                    plots[plotIndex].box.maxX else plots[plotIndex + (4 - (plotIndex % 5))].box.maxX

            return listOf(
                LorenzVec(xValueMin, playerPos.y, playerPos.z), LorenzVec(xValueMax, playerPos.y, playerPos.z)
            )
        } else if (xVelocity.absoluteValue < zVelocity.absoluteValue) {
            val zValueTop =
                if (plotIndex - 1 == -1 || !plots[plotIndex - 5].owned) plots[plotIndex].box.minZ else plots[plotIndex - 5].box.minZ
            val zValueBottom =
                if ((plotIndex + 1) % 5 == 0 || !plots[plotIndex + 1].owned) plots[plotIndex].box.maxZ else plots[plotIndex + 5].box.maxZ

            return listOf(
                LorenzVec(playerPos.x, playerPos.y, zValueTop), LorenzVec(playerPos.x, playerPos.y, zValueBottom)
            )
        } else if (xVelocity.absoluteValue == 0.0 && zVelocity.absoluteValue == 0.0) {
            return listOf()
        } else {
            return listOf()
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!config.enabled || !GardenAPI.inGarden()) return
        if (GardenCropSpeed.averageBlocksPerSecond <= 0.0 && config.farmingOnly) return
        val notificationSettings = config.notification.settings
        val plot = GardenPlotAPI.getCurrentPlot()

        if (plot != null) {
            val plotIndex = plots.indexOf(plot)
            val playerPos = LocationUtils.playerLocation()
            val farmEnd = getFarmBounds(plotIndex, playerPos, lastPos)
            blocksPerSecond = playerPos.distance(lastPos) * 20
            this.lastPos = playerPos

            if (farmEnd.isNotEmpty() && lastLaneSwitch.passedSince() >= ((farmEnd[0].distance(farmEnd[1]) / blocksPerSecond) + 3).seconds && (blocksPerSecond - lastBlocksPerSecond).absoluteValue <= 20) {
                if (farmEnd.any {switchPossibleInTime(playerPos, it, blocksPerSecond, notificationSettings.notificationThreshold)}) {
                    sendTitle(
                        config.notification.settings.notificationColor.getChatColor() + notificationSettings.notificationText,
                        notificationSettings.notificationDuration.seconds
                    )
                    playUserSound()
                    this.lastLaneSwitch = SimpleTimeMark.now()
                }
            }
            this.lastBlocksPerSecond = blocksPerSecond
        }
    }
}
