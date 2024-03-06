package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.events.CropClickEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object GardenStartLocation {

    private val config get() = GardenAPI.config.cropStartLocation

    fun setLocationCommand() {
        if (!GardenAPI.inGarden()) {
            ChatUtils.userError("This Command only works in the garden!")
            return
        }
        if (!config.enabled) {
            ChatUtils.clickableChat(
                "This feature is disabled. Enable it in the config: §e/sh crop start location",
                "sh crop start location",
                prefixColor = "§c"
            )
            return
        }

        val startLocations = GardenAPI.storage?.cropStartLocations
        if (startLocations == null) {
            ChatUtils.userError("The config is not yet loaded, retry in a second.")
            return
        }

        val crop = GardenAPI.getCurrentlyFarmedCrop()
        if (crop == null) {
            ChatUtils.userError("Hold a crop specific farming tool in the hand!")
            return
        }

        startLocations[crop] = LocationUtils.playerLocation()
        ChatUtils.chat("You changed your Crop Start Location for ${crop.cropName}!")
    }

    @SubscribeEvent
    fun onBlockClick(event: CropClickEvent) {
        if (!isEnabled()) return
        val startLocations = GardenAPI.storage?.cropStartLocations ?: return
        val crop = GardenAPI.getCurrentlyFarmedCrop() ?: return
        if (crop != GardenCropSpeed.lastBrokenCrop) return

        if (!startLocations.contains(crop)) {
            startLocations[crop] = LocationUtils.playerLocation()
            ChatUtils.chat("Auto updated your Crop Start Location for ${crop.cropName}")
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        val startLocations = GardenAPI.storage?.cropStartLocations ?: return
        val crop = GardenAPI.cropInHand ?: return
        val location = startLocations[crop]?.add(-0.5, 0.5, -0.5) ?: return

        event.drawWaypointFilled(location, LorenzColor.WHITE.toColor())
        event.drawDynamicText(location, crop.cropName, 1.5)
    }

    fun isEnabled() = GardenAPI.inGarden() && config.enabled
}
