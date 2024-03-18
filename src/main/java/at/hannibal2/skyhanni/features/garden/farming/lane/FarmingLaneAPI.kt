package at.hannibal2.skyhanni.features.garden.farming.lane

import at.hannibal2.skyhanni.events.CropClickEvent
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.farming.FarmingLaneSwitchEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object FarmingLaneAPI {
    val config get() = GardenAPI.config.farmingLane

    val lanes get() = GardenAPI.storage?.farmingLanes
    var currentLane: FarmingLane? = null
    private var lastNoLaneWarning = SimpleTimeMark.farPast()
    private var lastCrop: CropType? = null

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        handleLaneSwitch(event.crop)
    }

    @SubscribeEvent
    fun onCropClick(event: CropClickEvent) {
        handleLaneSwitch(event.crop)
    }

    private fun handleLaneSwitch(crop: CropType?) {
        if (crop == lastCrop) return
        lastCrop = crop

        val lanes = lanes ?: return
        val lane = lanes[crop]
        if (lane == null) {
            warnNoLane(crop)
        }

        if (currentLane == lane) return
        currentLane = lane
        FarmingLaneSwitchEvent(lane).postAndCatch()
    }

    private fun warnNoLane(crop: CropType?) {
        if (crop == null || currentLane != null) return
        if (!config.distanceDisplay && !config.laneSwitchNotification.enabled) return

        if (lastNoLaneWarning.passedSince() < 30.seconds) return
        lastNoLaneWarning = SimpleTimeMark.now()

        ChatUtils.clickableChat(
            "No ${crop.cropName} lane defined yet! Use §e/shlanedetection",
            command = "shlanedetection"
        )
    }

    fun FarmingDirection.getValue(location: LorenzVec): Double = when (this) {
        FarmingDirection.NORTH_SOUTH -> location.z
        FarmingDirection.EAST_WEST -> location.x
    }

    fun FarmingDirection.setValue(location: LorenzVec, value: Double): LorenzVec = when (this) {
        FarmingDirection.NORTH_SOUTH -> location.copy(z = value)
        FarmingDirection.EAST_WEST -> location.copy(x = value)
    }
}
