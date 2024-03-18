package at.hannibal2.skyhanni.features.garden.farming.lane

import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.garden.CropType
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
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object LaneDisplay {
    private val config get() = GardenAPI.config.laneswitch

    val lanes = mutableMapOf<CropType, FarmingLane>()

    var currentLane: FarmingLane? = null
    private var oldValue: Double? = null
    private var currentDirection = 0
    private var remainingDistance = 0.0
    private var lastValueSaved = SimpleTimeMark.farPast()

    private var display = listOf<String>()
    private var timeRemaining: Duration? = null

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        currentLane = lanes[event.crop]
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!Minecraft.getMinecraft().thePlayer.onGround) return

        if (!event.isMod(2)) return

        val currentLane = currentLane ?: return
        val direction = currentLane.direction
        val min = currentLane.min
        val max = currentLane.max
        val position = direction.getValue(LocationUtils.playerLocation())
        val outside = position !in min..max
        if (outside) return

        val oldValue = oldValue ?: run {
            oldValue = position
            return
        }
        val diff = oldValue - position
        LaneDisplay.oldValue = position

        if (lastValueSaved.passedSince() > 1.seconds) return
        lastValueSaved = SimpleTimeMark.now()

        if (diff > 0) {
            currentDirection = 1
        } else if (diff < 0) {
            currentDirection = -1
        }
        remainingDistance = when (currentDirection) {
            1 -> min - position
            -1 -> max - position
            else -> return
        }.absoluteValue
        sendWarning()

        display = buildList {
            add("§7Distance until Switch: §e${remainingDistance.round(1)}")
            add("§7Time remaining: ${timeRemaining?.format()}")
        }
    }

    private fun sendWarning() {
        val speedPerSecond = LocationUtils.distanceFromPreviousTick()
        if (speedPerSecond == 0.0) return
        val timeRemaining = (remainingDistance / speedPerSecond).seconds
        val settings = config.notification.settings
        if (timeRemaining >= settings.warnSeconds.seconds) return
        LaneDisplay.timeRemaining = timeRemaining

        with(settings) {
            LorenzUtils.sendTitle(color.getChatColor() + text, duration.seconds)
        }
        playUserSound()
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (!config.startEndWaypoints) return

        val lane = currentLane ?: return
        val direction = lane.direction
        val location = LocationUtils.playerLocation()
        val min = direction.setValue(location, lane.min)
        val max = direction.setValue(location, lane.max)

        if (currentDirection == 1) {
            event.drawWaypointFilled(min, LorenzColor.RED.toColor(), beacon = true)
            event.drawDynamicText(min, "§cEnd", 1.5)
        } else if (currentDirection == -1) {
            event.drawWaypointFilled(min, LorenzColor.GREEN.toColor(), beacon = true)
            event.drawDynamicText(min, "§aStart", 1.5)
        }
        if (currentDirection == 1) {
            event.drawWaypointFilled(max, LorenzColor.GREEN.toColor(), beacon = true)
            event.drawDynamicText(max, "§aStart", 1.5)
        } else if (currentDirection == -1) {
            event.drawWaypointFilled(max, LorenzColor.RED.toColor(), beacon = true)
            event.drawDynamicText(max, "§cEnd", 1.5)
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled() || !config.distanceUntilSwitch) return

        config.distanceUntilSwitchPosition.renderStrings(display, posLabel = "Lane Display")
    }
        @JvmStatic
        fun playUserSound() {
            SoundUtils.createSound(
                config.notification.sound.notificationSound,
                config.notification.sound.notificationPitch,
            ).playSound()
        }

    private fun isEnabled() = GardenAPI.isCurrentlyFarming() && config.enabled

}
