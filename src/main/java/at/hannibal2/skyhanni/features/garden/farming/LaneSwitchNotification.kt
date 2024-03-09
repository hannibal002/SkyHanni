package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.getCurrentPlot
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.isBarn
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.plots
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.LorenzUtils.sendTitle
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

class LaneSwitchNotification {

    private val config get() = GardenAPI.config.laneswitch

    private var blocksPerSecond = 0.0
    private var distancesUntilSwitch: List<Double> = listOf()
    private var lastBlocksPerSecond = 0.0
    private var lastPos = LorenzVec(0, 0, 0)
    private var lastLaneSwitch = SimpleTimeMark.farPast()
    private var lastWarning = SimpleTimeMark.farPast()
    private var lastDistancesUntilSwitch: List<Double> = listOf()
    private var lastDistance = 0.0

    companion object {
        private val config get() = GardenAPI.config.laneswitch

        @JvmStatic
        fun playUserSound() {
            SoundUtils.createSound(
                config.notification.sound.notificationSound,
                config.notification.sound.notificationPitch,
            ).playSound()
        }
    }

    private fun switchPossibleInTime(from: LorenzVec, to: LorenzVec, speed: Double, time: Int): Boolean {
        return from.distance(to) <= speed * time
    }

    private fun getFarmBounds(plotIndex: Int, playerPos: LorenzVec, lastPos: LorenzVec): List<LorenzVec>? {
        if(plots[plotIndex].isBarn() || plotIndex == 12) return null
        val xVelocity = playerPos.x - lastPos.x
        val zVelocity = playerPos.z - lastPos.z
        if (xVelocity.absoluteValue > zVelocity.absoluteValue) {
            val xValueMin = if (plotIndex - 1 == -1 || !plots[plotIndex - 1].unlocked || plots.indexOf(plots[plotIndex - 1]) == 12)
                plots[plotIndex].box.minX else plots[plotIndex - plotIndex % 5].box.minX
            val xValueMax = if ((plotIndex + 1) % 5 == 0 || plotIndex + 1 == 25 || !plots[plotIndex + 1].unlocked || plots.indexOf(plots[plotIndex + 1]) == 12)
                plots[plotIndex].box.maxX else plots[plotIndex + (4 - (plotIndex % 5))].box.maxX

            return listOf(
                LorenzVec(xValueMin, playerPos.y, playerPos.z), LorenzVec(xValueMax, playerPos.y, playerPos.z)
            )
        } else if (xVelocity.absoluteValue < zVelocity.absoluteValue) {
            val row = (ceil((plotIndex / 5.0) + 0.2) - 1).toInt()
            val zValueTop = if (plotIndex - 1 == -1 || plotIndex - 5 < 0 || !plots[plotIndex - 5].unlocked || plots.indexOf(plots[plotIndex - 5]) == 12)
                plots[plotIndex].box.minZ else plots[plotIndex - (5 * row)].box.minZ
            val zValueBottom = if (plotIndex + 5 > 24 || !plots[plotIndex + 5].unlocked || plots.indexOf(plots[plotIndex + 5]) == 12)
                plots[plotIndex].box.maxZ else plots[plotIndex + ((4 - row)) * 5].box.maxZ

            return listOf(
                LorenzVec(playerPos.x, playerPos.y, zValueTop), LorenzVec(playerPos.x, playerPos.y, zValueBottom)
            )
        } else if (xVelocity.absoluteValue == 0.0 && zVelocity.absoluteValue == 0.0) {
            return null
        } else {
            return null
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!config.enabled || !GardenAPI.inGarden() || !plotsLoaded() || !isFarming()) return
        val notificationSettings = config.notification.settings
        val plot = getCurrentPlot() ?: return

        val plotIndex = plots.indexOf(plot)
        val playerPos = LocationUtils.playerLocation()
        val farmEnd = getFarmBounds(plotIndex, playerPos, lastPos) ?: return
        val farmLength = farmEnd[0].distance(farmEnd[1])
        lastPos = playerPos
        blocksPerSecond = LocationUtils.distanceFromPreviousTick()
        distancesUntilSwitch = farmEnd.map { end -> end.distance(playerPos).round(2) }

        val farmTraverseTime = ((farmLength / blocksPerSecond) - (notificationSettings.notificationThreshold * 2)).seconds
        val bpsDifference = (blocksPerSecond - lastBlocksPerSecond).absoluteValue

        if (farmEnd.isNotEmpty() && lastLaneSwitch.passedSince() >= farmTraverseTime && bpsDifference <= 20) {
            if (farmEnd.any {switchPossibleInTime(playerPos, it, blocksPerSecond, notificationSettings.notificationThreshold)}) {
                sendTitle(
                    config.notification.settings.notificationColor.getChatColor() + notificationSettings.notificationText,
                    notificationSettings.notificationDuration.seconds
                )
                playUserSound()
                lastLaneSwitch = SimpleTimeMark.now()
            }
        }
        lastBlocksPerSecond = blocksPerSecond
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.distanceUntilSwitch || !GardenAPI.inGarden() || !plotsLoaded() || !isFarming()) return
        if (distancesUntilSwitch.isEmpty()) return
        if (lastDistancesUntilSwitch.isEmpty()) { lastDistancesUntilSwitch = distancesUntilSwitch }

        val distances = listOf(distancesUntilSwitch[0] - lastDistancesUntilSwitch[0], distancesUntilSwitch[1] - lastDistancesUntilSwitch[1])
        val distance = if (distances.all { it != 0.0 }) {
            if (distances[0] > 0) distancesUntilSwitch[1] else distancesUntilSwitch[0]
        } else {
            lastDistance
        }

        config.distanceUntilSwitchPos.renderString("Distance until Switch: $distance", posLabel = "Movement Speed")
        lastDistancesUntilSwitch = distancesUntilSwitch
        lastDistance = distance
    }

    private fun plotsLoaded(): Boolean {
        if (!plots.any { it.unlocked } && lastWarning.passedSince() >= 30.seconds) {
            ChatUtils.clickableChat("§eOpen your configure plots for lane switch detection to work.", "/desk")
            lastWarning = SimpleTimeMark.now()
            return false
        }
        return true
    }

    private fun isFarming(): Boolean = GardenCropSpeed.averageBlocksPerSecond <= 0.0
}
