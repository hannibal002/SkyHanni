package at.hannibal2.skyhanni.config.features.crimsonisle

import at.hannibal2.skyhanni.config.ConfigEditorKeyMapping
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.InputKeys
import com.google.gson.annotations.Expose
import com.mojang.blaze3d.platform.InputConstants
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class VanquisherWaypointShareConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Share your Vanquisher spawns and receive other Vanquisher spawns via Party Chat.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Instant Share", desc = "Instantly share your Vanquisher spawns via Party Chat.")
    @ConfigEditorBoolean
    var instantShare: Boolean = true

    @Expose
    @ConfigOption(name = "Keybind Share", desc = "Manually share your Vanquisher spawns with a keybind.")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_Y)
    var keybindSharing = InputKeys.KEY_Y

    @Expose
    @ConfigOption(name = "Read Global Chat", desc = "Register Vanquisher spawns from All Chat.")
    @ConfigEditorBoolean
    var readGlobalChat: Boolean = false
}
