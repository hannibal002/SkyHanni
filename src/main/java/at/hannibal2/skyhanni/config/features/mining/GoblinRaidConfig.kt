package at.hannibal2.skyhanni.config.features.mining

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GoblinRaidConfig {
    @Expose
    @ConfigOption(
        name = "Superprotectron Highlight",
        desc = "Highlights the Superprotectron to make it easier to spot in the crowd.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val superProtectronHighlight: Boolean = true

    @Expose
    @ConfigOption(
        name = "Highlight Color",
        desc = "The color of the Superprotectron highlight.",
    )
    @ConfigEditorColour
    var superprotectronHighlightColor: String = "0:255:255:0:88"
}
