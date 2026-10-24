package at.hannibal2.skyhanni.config.features.crimsonisle

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW

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
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_Y)
    var keybindSharing: Int = GLFW.GLFW_KEY_Y

    @Expose
    @ConfigOption(name = "Read Global Chat", desc = "Register Vanquisher spawns from All Chat.")
    @ConfigEditorBoolean
    var readGlobalChat: Boolean = false

    @ConfigOption(
        name = "Invincibility Timer",
        desc = "Click to open the Invincibility Timer settings."
    )
    @ConfigEditorButton(buttonText = "OPEN")
    val invincibilityTracker: Runnable = Runnable { SkyHanniMod.feature.combat.mobs.invincibilityTimer::enabled.jumpToEditor() }
}
