package at.hannibal2.skyhanni.config.features.combat

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FireFreezeConfig {

    @Expose
    @ConfigOption(name = "Mob Timer", desc = "Shows Freeze timer Near Mobs.")
    @ConfigEditorBoolean
    @FeatureToggle
    var mobTimer: Boolean = false

    @Expose
    @ConfigOption(name = "Custom Circle", desc = "Replaces Fire Freeze Particles with a Circular Line.")
    @ConfigEditorBoolean
    @FeatureToggle
    var customCylinder: Boolean = false

    @Expose
    @ConfigOption(name = "Box Frozen Mobs", desc = "Box Frozen Mobs.")
    @ConfigEditorBoolean
    @FeatureToggle
    var mobHighlight: Boolean = false

}
