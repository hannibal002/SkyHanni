package at.hannibal2.skyhanni.config.features.mining

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import java.awt.Color

class GoblinRaidConfig {

    @Expose
    @ConfigOption(
        name = "Superprotectron Highlight",
        desc = "Highlights the Superprotectron to make it easier to spot in the crowd.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var superprotectronHighlight: Boolean = true

    @Expose
    @ConfigOption(
        name = "Highlight Color",
        desc = "The color of the Superprotectron highlight.",
    )
    @ConfigEditorColour
    var superprotectronHighlightColor: ChromaColour = Color.YELLOW.toChromaColor(88)
}
