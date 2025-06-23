package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.OnlyLegacy
import at.hannibal2.skyhanni.config.OnlyModern
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

/**
 * Attention developers:
 * If your feature can only be used on the foraging islands please mark it with @[OnlyModern]
 */
class ForagingConfig {

    @ConfigOption(
        name = "§cNotice",
        desc = "To see all foraging features please launch the game on a modern version of Minecraft with SkyHanni installed."
    )
    @OnlyLegacy
    @ConfigEditorInfoText
    var notice: String = ""

    @Expose
    @ConfigOption(name = "Moonglade Beacon", desc = "Settings for the moonglade beacon.")
    @OnlyModern
    @Accordion
    var moongladeBeacon = MoongladeBeaconConfig()

    @Expose
    @ConfigOption(name = "Birries Highlight", desc = "")
    @OnlyModern
    @Accordion
    var birriesHighlight = BirriesHighlightConfig()

}
