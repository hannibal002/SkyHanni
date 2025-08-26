package at.hannibal2.skyhanni.config.features.combat.damageindicator

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class EnderSlayerConfig {
    @Expose
    @ConfigOption(name = "Laser Phase Timer", desc = "Show a timer for when the laser phase will end.")
    @ConfigEditorBoolean
    @FeatureToggle
    var laserPhaseTimer: Boolean = false

    // TODO test what this does exactly
    @Expose
    @ConfigOption(name = "Health During Laser", desc = "Show the health of Voidgloom Seraph 4 in Damage Indicator during the laser phase .")
    @ConfigEditorBoolean
    var showHealthDuringLaser: Boolean = false
}
