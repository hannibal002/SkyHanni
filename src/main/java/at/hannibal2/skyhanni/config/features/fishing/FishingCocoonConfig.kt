package at.hannibal2.skyhanni.config.features.fishing

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FishingCocoonConfig {
    @Expose
    @ConfigOption(name = "Share In Chat", desc = "Send the Cocooned Mob Info into Party Chat")
    @ConfigEditorBoolean
    @FeatureToggle
    var shareInPartyChat: Boolean = false

    @Expose
    @ConfigOption(name = "Send Title", desc = "Show the Cocooned mob as a Title to Warn When Cocooning X mob.")
    @ConfigEditorBoolean
    @FeatureToggle
    var warnWhenCocooned: Boolean = false

    @ConfigOption(name = "Custom Cocoon Settings", desc = "Both Above Features can be customized under /shseacreatures!")
    @ConfigEditorInfoText
    var notice: String = ""
}
