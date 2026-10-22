package at.hannibal2.skyhanni.config.features.dev

import at.hannibal2.skyhanni.config.ConfigEditorKeyMapping
import com.google.gson.annotations.Expose
import com.mojang.blaze3d.platform.InputConstants
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class WaypointsConfig {
    @Expose
    @ConfigOption(
        name = "Save Hotkey",
        desc = "Saves block location to a temporary parkour and copies everything to your clipboard."
    )
    @ConfigEditorKeyMapping
    var saveKey = InputConstants.UNKNOWN

    @Expose
    @ConfigOption(name = "Delete Hotkey", desc = "Deletes the last saved location for when you make a mistake.")
    @ConfigEditorKeyMapping
    var deleteKey = InputConstants.UNKNOWN

    @Expose
    @ConfigOption(name = "Show Platform Number", desc = "Show the index number over the platform for every parkour.")
    @ConfigEditorBoolean
    var showPlatformNumber: Boolean = false

    @Expose
    @ConfigOption(name = "Show Outside SkyBlock", desc = "Make parkour waypoints outside of SkyBlock too.")
    @ConfigEditorBoolean
    var parkourOutsideSB: Boolean = false
}
