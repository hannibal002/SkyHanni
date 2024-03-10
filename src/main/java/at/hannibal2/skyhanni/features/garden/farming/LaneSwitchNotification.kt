package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI
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

    private fun isBoundaryPlot(plotIndex: Int, direction: Direction, value: Value): Boolean {
        if (direction == Direction.WEST_EAST) {
            val isNextNewRow: Boolean
            val isNextUnlocked: Boolean
            val isNextBarn: Boolean
            if (value == Value.MIN) {
                if (plotIndex - 1 == -1) return true // check if next plot is out of bounds
                isNextNewRow = plots[plotIndex - 1].box.maxX.absoluteValue.round(0) == 240.0 //Check if the next plot's border is 240 and therefore in the previous row
                isNextUnlocked = plots[plotIndex - 1].unlocked
                isNextBarn = plots[plotIndex - 1].isBarn()
            } else {
                if (plotIndex + 1 == 25) return true // check if next plot is out of bounds
                isNextNewRow = (plotIndex + 1) % 5 == 0
                isNextUnlocked = plots[plotIndex + 1].unlocked
                isNextBarn = plots[plotIndex + 1].isBarn()
            }
            return isNextNewRow || !isNextUnlocked || isNextBarn
        } else if (direction == Direction.NORTH_SOUTH){
            val isNextUnlocked: Boolean
            val isNextBarn: Boolean

            if (value == Value.MAX) {
                if (plotIndex - 1 == -1 || (plotIndex - 5) < 0) return true // check if next plot is out of bounds
                isNextUnlocked = plots[plotIndex - 5].unlocked
                isNextBarn = plots[plotIndex - 5].isBarn()
            } else {
                if (plotIndex + 5 > 24) return true // check if next plot is out of bounds
                isNextUnlocked = plots[plotIndex + 5].unlocked
                isNextBarn = plots[plotIndex + 5].isBarn()
            }
            return !isNextUnlocked || isNextBarn
        }
        return false
    }

    enum class Direction {
        WEST_EAST,
        NORTH_SOUTH,
        ;
    }

    enum class Value {
        MIN,
        MAX,
        TOP,
        BOTTOM,
        ;
    }

    private fun getFarmBounds(plotIndex: Int, playerPos: LorenzVec, lastPos: LorenzVec): List<LorenzVec>? {
        if(plots[plotIndex].isBarn() || plotIndex == 12) return null
        val xVelocity = playerPos.x - lastPos.x
        val zVelocity = playerPos.z - lastPos.z
        if (xVelocity.absoluteValue > zVelocity.absoluteValue) {
            var xValueMin = 0.0
            var xValueMax = 0.0

            for(i in 0..4) {
                if(isBoundaryPlot(plotIndex - i, Direction.WEST_EAST, Value.MIN)) { xValueMin = plots[plotIndex - i].box.minX; break }
            }
            for(i in 0..4) {
                if(isBoundaryPlot(plotIndex + i, Direction.WEST_EAST, Value.MAX)) { xValueMax = plots[plotIndex + i].box.maxX; break }
            }

            return listOf(
                LorenzVec(xValueMin, playerPos.y, playerPos.z), LorenzVec(xValueMax, playerPos.y, playerPos.z)
            )
        } else if (xVelocity.absoluteValue < zVelocity.absoluteValue) {
            // i * 5 because going vertically is always 5 plots before or after the current
            var zValueTop = 0.0
            var zValueBottom = 0.0

            for(i in 0..4) {
                if(isBoundaryPlot(plotIndex - (i * 5), Direction.NORTH_SOUTH, Value.TOP)) { zValueTop = plots[plotIndex - (i * 5)].box.minZ; break }
            }
            for(i in 0..4) {
                if(isBoundaryPlot(plotIndex + (i * 5), Direction.NORTH_SOUTH, Value.BOTTOM)) { zValueBottom = plots[plotIndex + (i * 5)].box.maxZ; break }
            }

            return listOf(
                LorenzVec(playerPos.x, playerPos.y, zValueTop), LorenzVec(playerPos.x, playerPos.y, zValueBottom)
            )
        }
        return null
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        val notificationSettings = config.notification.settings
        val plot = GardenPlotAPI.getCurrentPlot() ?: return
        if (!plot.unlocked) return

        val plotIndex = plots.indexOf(plot)
        val playerPos = LocationUtils.playerLocation()
        val farmEnd = getFarmBounds(plotIndex, playerPos, lastPos) ?: return
        val farmLength = farmEnd[0].distance(farmEnd[1])
        lastPos = playerPos
        blocksPerSecond = LocationUtils.distanceFromPreviousTick()
        distancesUntilSwitch = farmEnd.map { end -> end.distance(playerPos).round(2) }

        // farmLength / bps to get the time needed to travel the distance, - the threshold times the farm length divided by the length of 2 plots (to give some room)
        val farmTraverseTime = ((farmLength / blocksPerSecond) - (notificationSettings.notificationThreshold * (farmLength / 192))).seconds
        val bpsDifference = (blocksPerSecond - lastBlocksPerSecond).absoluteValue

        if (farmEnd.isNotEmpty() && lastLaneSwitch.passedSince() >= farmTraverseTime && bpsDifference <= 20) {
            if (farmEnd.any {switchPossibleInTime(playerPos, it, blocksPerSecond, notificationSettings.notificationThreshold)}) {
                sendTitle(
                    notificationSettings.notificationColor.getChatColor() + notificationSettings.notificationText,
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
        if (!config.distanceUntilSwitch || !isEnabled()) return
        if (distancesUntilSwitch.isEmpty()) return
        if (lastDistancesUntilSwitch.isEmpty()) { lastDistancesUntilSwitch = distancesUntilSwitch }

        val distances = listOf(distancesUntilSwitch[0] - lastDistancesUntilSwitch[0], distancesUntilSwitch[1] - lastDistancesUntilSwitch[1]) //Get changes in the distances
        val distance = if (distances.all { it != 0.0 }) {
            if (distances[0] > 0) distancesUntilSwitch[1] else distancesUntilSwitch[0] // get the direction the player is traveling and get the distance to display from that
        } else {
            lastDistance // display last value if no change is detected
        }

        config.distanceUntilSwitchPos.renderString("Distance until Switch: $distance", posLabel = "Movement Speed")
        lastDistancesUntilSwitch = distancesUntilSwitch
        lastDistance = distance
    }

    private fun plotsLoaded(): Boolean {
        if (!plots.any { it.unlocked }) {
            if (lastWarning.passedSince() >= 30.seconds) {
                ChatUtils.clickableChat("§eOpen your configure plots for lane switch detection to work.", "/desk")
                lastWarning = SimpleTimeMark.now()
            }
            return false
        }
        return true
    }

    fun isEnabled(): Boolean = config.enabled && GardenAPI.inGarden() && plotsLoaded() && GardenCropSpeed.averageBlocksPerSecond > 0.0
}
