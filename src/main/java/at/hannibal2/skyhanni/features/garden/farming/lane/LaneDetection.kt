package at.hannibal2.skyhanni.features.garden.farming.lane

import at.hannibal2.skyhanni.events.CropClickEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.farming.LaneDisplay
import at.hannibal2.skyhanni.features.garden.farming.lane.FarmingLaneAPI.getValue
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

object LaneDetection {

    private var laneDetection = false
    private var start: LorenzVec? = null
    private var lastLocation: LorenzVec? = null
    private var potentialEnd: LorenzVec? = null
    private var crop: CropType? = null
    private var maxDistance = 0.0

    fun toggleCommand() {
        laneDetection = !laneDetection
        if (laneDetection) {
            ChatUtils.chat("Enabled lane detection. Start farming one layer until we say to stop.")
        } else {
            ChatUtils.chat("Stopped lane detection.")
        }
    }

    @SubscribeEvent
    fun onCropClick(event: CropClickEvent) {
        if (!isEnabled()) return

        val location = LocationUtils.playerLocation()

        val lastLocation = lastLocation ?: run {
            start = location
            maxDistance = 0.0
            lastLocation = location
            crop = event.crop
            return
        }

        if (crop != event.crop) {
            ChatUtils.chat("Different crop broken, stopping lane detection")
            reset()
            return
        }
        if (lastLocation.distance(location) < 1) return

        this.lastLocation = location
        val start = start ?: error("start can not be null")
        val distance = start.distance(location)
        if (distance > maxDistance) {
            maxDistance = distance
            potentialEnd = null
        } else {
            val potentialEnd = potentialEnd ?: run {
                potentialEnd = location
                return
            }
            if (potentialEnd.distance(location) > 5) {
                val crop = crop ?: error("crop can not be null")
                saveLane(start, potentialEnd, crop)
            }
        }
    }

    private fun saveLane(a: LorenzVec, b: LorenzVec, crop: CropType) {
        val lane = createLane(a, b)
        LaneDisplay.lanes[crop] = lane
        LaneDisplay.currentLane = lane
        ChatUtils.chat("${crop.cropName} lane saved. You can stop now.")
        reset()
    }

    private fun createLane(a: LorenzVec, b: LorenzVec): FarmingLane {
        val diffX = a.x - b.x
        val diffZ = a.z - b.z
        val direction = if (diffZ.absoluteValue > diffX.absoluteValue) FarmingDirection.NORTH_SOUTH else FarmingDirection.OST_WEST

        val min = min(direction.getValue(a), direction.getValue(b))
        val max = max(direction.getValue(a), direction.getValue(b))

        return FarmingLane(direction, min, max)
    }

    private fun reset() {
        start = null
        lastLocation = null
        crop = null
        maxDistance = 0.0
        laneDetection = false
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return

        start?.let {
            event.drawWaypointFilled(it, LorenzColor.WHITE.toColor(), beacon = true)
            event.drawDynamicText(it, "start", 1.5)
        }
        lastLocation?.let {
            event.drawWaypointFilled(it, LorenzColor.WHITE.toColor(), beacon = true)
            event.drawDynamicText(it, "lastLocation", 1.5)
        }
        potentialEnd?.let {
            event.drawWaypointFilled(it, LorenzColor.WHITE.toColor(), beacon = true)
            event.drawDynamicText(it, "potentialEnd", 1.5)
        }
    }

    private fun isEnabled() = GardenAPI.inGarden() && laneDetection
}
