package at.hannibal2.skyhanni.config.features.crimsonisle

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW

class VanquisherWaypointShareConfig {
    //Share vanquisher spawns
    @Expose
    @ConfigOption(name = "Enabled", desc = "Share your Vanquisher spawns and receive other Vanquisher spawns via Party Chat")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    //Instant share w/o keybind
    @Expose
    @ConfigOption(name = "Instant Share", desc = "Instantly share your Vanquisher spawns via Party Chat")
    @ConfigEditorBoolean
    var instantShare: Boolean = true

    //Manually share with keybind
    @Expose
    @ConfigOption(name = "Keybind Share", desc = "Manually share your Vanquisher spawns with a keybind")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_Y)
    var keybindSharing: Int = GLFW.GLFW_KEY_Y

    //Read global chat to recognize non-party members' Vanquisher spawns
    @Expose
    @ConfigOption(name = "Read Global Chat", desc = "Register Vanquisher spawns from All Chat and Party Chat")
    @ConfigEditorBoolean
    var readGlobalChat: Boolean = false
}
