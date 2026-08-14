package at.hannibal2.skyhanni.config.features.slayer.endermen

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.generic.LineToConfig
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class EndermanBeaconConfig {

    @Expose
    @ConfigOption(name = "Show a Line to Crosshair From Beacon", desc = "")
    @Accordion
    val line: LineToConfig = LineToConfig(defaultColor = ChromaColour.fromStaticRGB(255, 0, 88, 255))

    @Expose
    @ConfigOption(
        name = "Highlight Beacon",
        desc = "Highlight the Enderman Slayer Yang Glyph (beacon) in red color and added a timer for when he explodes.\n" +
            "Supports beacon in hand and beacon flying."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightBeacon: Boolean = true

    @Expose
    @ConfigOption(name = "Beacon Color", desc = "Color of the beacon.")
    @ConfigEditorColour
    var beaconColor: ChromaColour = ChromaColour.fromStaticRGB(255, 0, 88, 255)

    @Expose
    @ConfigOption(
        name = "Show Warning",
        desc = "Display a warning mid-screen when the Enderman Slayer throws a Yang Glyph (beacon)."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var showWarning: Boolean = false


    @SkyHanniModule
    companion object {
        @HandleEvent
        fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
            event.move(
                3,
                "slayer.endermanBeaconConfig.highlightBeacon",
                "slayer.endermen.endermanBeaconConfig.highlightBeacon",
            )
            event.move(3, "slayer.endermanBeaconConfig.beaconColor", "slayer.endermen.endermanBeaconConfig.beaconColor")
            event.move(3, "slayer.endermanBeaconConfig.showWarning", "slayer.endermen.endermanBeaconConfig.showWarning")
            event.move(3, "slayer.endermanBeaconConfig.showLine", "slayer.endermen.endermanBeaconConfig.showLine")
            event.move(3, "slayer.endermanBeaconConfig.lneColor", "slayer.endermen.endermanBeaconConfig.lineColor")
            event.move(3, "slayer.endermanBeaconConfig.lineWidth", "slayer.endermen.endermanBeaconConfig.lineWidth")
            event.move(9, "slayer.enderman.endermanBeaconConfig", "slayer.endermen.beacon")
            event.move(143, "slayer.endermen.beacon.showLine", "slayer.endermen.beacon.line.showLine")
            event.move(143, "slayer.endermen.beacon.lineColor", "slayer.endermen.beacon.line.color")
            event.move(143, "slayer.endermen.beacon.lineWidth", "slayer.endermen.beacon.line.lineWidth")
        }
    }
}
