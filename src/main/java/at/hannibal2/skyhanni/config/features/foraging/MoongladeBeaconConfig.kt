package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.OnlyModern
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MoongladeBeaconConfig {

    @Expose
    @ConfigOption(name = "Beacon Solver", desc = "Shows which slots to click on to solve the beacon.")
    @OnlyModern
    @FeatureToggle
    @ConfigEditorBoolean
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Use Middle Click", desc = "Click on slots with middle click to speed up interactions.")
    @OnlyModern
    @ConfigEditorBoolean
    var useMiddleClick: Boolean = true

    @Expose
    @ConfigOption(name = "Prevent Over-Clicking", desc = "Prevents clicking on a slot that already is set correctly.")
    @OnlyModern
    @ConfigEditorBoolean
    var preventOverClicking: Boolean = true

    @Expose
    @ConfigOption(name = "Alert when ready", desc = "Sends a title when the moonglade beacon is ready to be activated.")
    @OnlyModern
    @FeatureToggle
    @ConfigEditorBoolean
    var beaconAlert: Boolean = true

    @Expose
    @ConfigLink(owner = MoongladeBeaconConfig::class, field = "enabled")
    val displayPosition: Position = Position(-300, 140)

}
