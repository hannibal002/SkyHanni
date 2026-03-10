package at.hannibal2.skyhanni.config.features.combat

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CocoonOverlayConfig {

    @Expose
    @ConfigOption(name = "Show Timer", desc = "Shows Time Left till Cocoon Hatches, Can be inaccurate.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showCocoonTimerTillHatch: Boolean = true

    @Expose
    @ConfigOption(name = "Show Mob Name", desc = "Shows Mob Contained Within Cocoon's Name.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showCocoonContainedMobName: Boolean = true

}
