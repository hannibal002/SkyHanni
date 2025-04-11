package at.hannibal2.skyhanni.config.features.rift.area.westvillage

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class KloonHackingConfig {
    @Expose
    @ConfigOption(name = "Hacking Solver", desc = "Highlight the correct button to click in the hacking inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    var solver: Boolean = true

    @Expose
    @ConfigOption(name = "Color Guide", desc = "Show which color to pick.")
    @ConfigEditorBoolean
    @FeatureToggle
    var color: Boolean = true

    @Expose
    @ConfigOption(
        name = "Terminal Waypoints",
        desc = "While wearing the helmet, waypoints will appear at each terminal location."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var waypoints: Boolean = true
}
