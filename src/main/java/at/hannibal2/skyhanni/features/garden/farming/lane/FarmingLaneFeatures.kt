package at.hannibal2.skyhanni.features.garden.farming.lane

import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.farming.FarmingLaneSwitchEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.farming.lane.FarmingLaneAPI.getValue
import at.hannibal2.skyhanni.features.garden.farming.lane.FarmingLaneAPI.setValue
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object FarmingLaneFeatures {
    val config get() = FarmingLaneAPI.config

    private var oldValue: Double? = null
    private var remainingDistance = 0.0

    private var display = listOf<String>()
    private var timeRemaining: Duration? = null
    private var lastSpeed = 0.0
    private var validSpeed = false
    private var lastTimeFarming = SimpleTimeMark.farPast()
    private var lastDirection = 0

    @SubscribeEvent
    fun onFarmingLaneSwitch(event: FarmingLaneSwitchEvent) {
        display = emptyList()
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!GardenAPI.inGarden()) return
        if (!event.isMod(2)) return

        val lane = FarmingLaneAPI.currentLane ?: return
        val direction = lane.direction
        val min = lane.min
        val max = lane.max
        val position = direction.getValue(LocationUtils.playerLocation())
        val outside = position !in min..max
        if (outside) {
            display = emptyList()
            return
        }

        val oldValue = oldValue ?: run {
            oldValue = position
            return
        }
        val diff = oldValue - position
        FarmingLaneFeatures.oldValue = position

        val newDirection = if (diff > 0) {
            1
        } else if (diff < 0) {
            -1
        } else {
            0
        }

        remainingDistance = when (newDirection) {
            1 -> (min - position).absoluteValue
            -1 -> (max - position).absoluteValue
            else -> remainingDistance
        }

        if (newDirection != lastDirection) {
            // reset farming time, to prevent wrong lane warnings
            lastTimeFarming = SimpleTimeMark.farPast()
            lastDirection = newDirection
        }

        if (!GardenAPI.isCurrentlyFarming()) return

        if (config.distanceDisplay) {
            display = buildList {
                add("§7Distance until Switch: §e${remainingDistance.round(1)}")
                val color = if (validSpeed) "§b" else "§8"
                val timeRemaining = timeRemaining ?: return@buildList
                val format = timeRemaining.format(showMilliSeconds = timeRemaining < 5.seconds)
                add("§7Time remaining: $color$format")
            }
        }
        if (config.laneSwitchNotification.enabled) {
            sendWarning()
        }
    }

    private fun sendWarning() {
        val speedPerSecond = LocationUtils.distanceFromPreviousTick().round(2)
        if (speedPerSecond == 0.0) return
        val speedTooSlow = speedPerSecond < 1
        if (speedTooSlow) {
            validSpeed = false
            return
        }
        // only calculate the time if the speed has not changed
        if (lastSpeed != speedPerSecond) {
            lastSpeed = speedPerSecond
            validSpeed = false
            return
        }
        validSpeed = true

        val timeRemaining = (remainingDistance / speedPerSecond).seconds
        val switchSettings = config.laneSwitchNotification
        FarmingLaneFeatures.timeRemaining = timeRemaining + 1.seconds
        val warnAt = switchSettings.secondsBefore.seconds
        if (timeRemaining >= warnAt) {
            lastTimeFarming = SimpleTimeMark.now()
            return
        }

        // When the player was not inside the farm previously
        if (lastTimeFarming.passedSince() > warnAt) return

        with(switchSettings) {
            LorenzUtils.sendTitle(text.replace("&", "§"), 2.seconds)
        }
        playUserSound()
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!GardenAPI.inGarden()) return
        if (!config.cornerWaypoints) return

        val lane = FarmingLaneAPI.currentLane ?: return
        val direction = lane.direction
        val location = LocationUtils.playerLocation()
        val min = direction.setValue(location, lane.min)
        val max = direction.setValue(location, lane.max)

        event.drawWaypointFilled(min, LorenzColor.YELLOW.toColor(), beacon = true)
        event.drawDynamicText(min, "§eLane Corner", 1.5)
        event.drawWaypointFilled(max, LorenzColor.YELLOW.toColor(), beacon = true)
        event.drawDynamicText(max, "§eLane Corner", 1.5)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!GardenAPI.inGarden()) return
        if (!config.distanceDisplay) return

        config.distanceDisplayPosition.renderStrings(display, posLabel = "Lane Display")
    }

    @JvmStatic
    fun playUserSound() {
        with(config.laneSwitchNotification.sound) {
            SoundUtils.createSound(name, pitch).playSound()
        }
    }
}
