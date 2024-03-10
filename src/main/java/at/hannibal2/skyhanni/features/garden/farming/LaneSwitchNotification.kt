package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.config.features.garden.laneswitch.LaneSwitchNotificationSettings
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI
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

    private var bps = 0.0 // Blocks per Second
    private var distancesUntilSwitch: List<Double> = listOf()
    private var lastBps = 0.0 // Last blocks per Second
    private var lastPosition = LorenzVec(0, 0, 0)
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

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        val settings = config.notification.settings
        val plot = GardenPlotAPI.getCurrentPlot() ?: return
        if (!plot.unlocked) return

        val plotIndex = plots.indexOf(plot)
        val positon = LocationUtils.playerLocation()
        val farmEnd = LaneSwitchUtils.getFarmBounds(plotIndex, positon, lastPosition) ?: return
        val farmLength = farmEnd[0].distance(farmEnd[1])
        lastPosition = positon
        bps = LocationUtils.distanceFromPreviousTick()
        distancesUntilSwitch = farmEnd.map { end -> end.distance(positon).round(2) }

        testForLaneSwitch(settings, farmLength, farmEnd, positon)
        lastBps = bps
    }

    private fun testForLaneSwitch(
        settings: LaneSwitchNotificationSettings,
        farmLength: Double,
        farmEnd: List<LorenzVec>,
        positon: LorenzVec,
    ) {
        // farmLength / bps to get the time needed to travel the distance, - the threshold times the farm length divided by the length of 2 plots (to give some room)
        val threshold = settings.threshold
        // TODO find a name for this variable
        val FIND_A_NAME_FOR_ME = threshold * (farmLength / 192)
        val farmTraverseTime = ((farmLength / bps) - FIND_A_NAME_FOR_ME).seconds
        val bpsDifference = (bps - lastBps).absoluteValue

        if (farmEnd.isEmpty() || lastLaneSwitch.passedSince() < farmTraverseTime || bpsDifference > 20) return
        if (!farmEnd.any { switchPossibleInTime(positon, it, bps, threshold) }) return

        with(settings) {
            sendTitle(color.getChatColor() + text, duration.seconds)
        }
        playUserSound()
        lastLaneSwitch = SimpleTimeMark.now()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.distanceUntilSwitch || !isEnabled()) return
        if (distancesUntilSwitch.isEmpty()) return
        if (lastDistancesUntilSwitch.isEmpty()) {
            lastDistancesUntilSwitch = distancesUntilSwitch
        }

        val distances = listOf(
            distancesUntilSwitch[0] - lastDistancesUntilSwitch[0],
            distancesUntilSwitch[1] - lastDistancesUntilSwitch[1]
        ) //Get changes in the distances
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
        if (plots.any { it.unlocked }) return true

        if (lastWarning.passedSince() >= 30.seconds) {
            ChatUtils.clickableChat("§eOpen your configure plots for lane switch detection to work.", "/desk")
            lastWarning = SimpleTimeMark.now()
        }
        return false
    }

    private fun isEnabled() = GardenAPI.isCurrentlyFarming() && config.enabled && plotsLoaded()
}
