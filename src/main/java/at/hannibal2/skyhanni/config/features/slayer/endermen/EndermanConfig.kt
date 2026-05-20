package at.hannibal2.skyhanni.config.features.slayer.endermen

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.generic.LineToConfig
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class EndermanConfig {
    @Expose
    @ConfigOption(name = "Yang Glyph (Beacon)", desc = "")
    @Accordion
    val beacon: EndermanBeaconConfig = EndermanBeaconConfig()

    @Expose
    @ConfigOption(name = "Highlight Nukekubi Skulls", desc = "Highlight the Enderman Slayer Nukekubi Skulls (Eyes).")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightNukekebi: Boolean = false

    @Expose
    @ConfigOption(name = "Line To Nukekubi Skulls", desc = "")
    @Accordion
    val lineToNukekebi: LineToConfig = LineToConfig(defaultColor = LorenzColor.GOLD.toChromaColor())


    @Expose
    @ConfigOption(name = "Phase Display", desc = "Show the current phase of the Enderman Slayer.")
    @ConfigEditorBoolean
    var phaseDisplay: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Particles", desc = "Hide particles around Enderman Slayer bosses and Mini-Bosses.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideParticles: Boolean = false

    @SkyHanniModule
    companion object {
        @HandleEvent
        fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
            event.move(3, "slayer.endermanHighlightNukekebi", "slayer.endermen.highlightNukekebi")
            event.move(134, "slayer.endermen.drawLineToNukekebi", "slayer.endermen.drawLineToNukekebi")
        }
    }
}
