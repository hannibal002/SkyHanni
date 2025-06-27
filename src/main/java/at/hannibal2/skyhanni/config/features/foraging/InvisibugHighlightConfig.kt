package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.OnlyModern
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import java.awt.Color

class InvisibugHighlightConfig {
    @Expose
    @ConfigOption(name = "Highlight Invisibug", desc = "Highlights nearby Invisibug.")
    @FeatureToggle
    @ConfigEditorBoolean
    @OnlyModern
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Color", desc = "Color for the Hideonleaf highlight")
    @ConfigEditorColour
    @OnlyModern
    var color: ChromaColour = Color.CYAN.toChromaColor()
}
