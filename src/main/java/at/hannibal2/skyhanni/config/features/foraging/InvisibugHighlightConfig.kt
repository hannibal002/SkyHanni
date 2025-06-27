package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.OnlyModern
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import java.awt.Color

/**
 * Code stolen from @NopoTheGamer (im lazy)
 */
class InvisibugHighlightConfig {
    @Expose
    @ConfigOption(name = "Highlight Invisibugs", desc = "Highlights nearby Invisibugs.")
    @FeatureToggle
    @ConfigEditorBoolean
    @OnlyModern
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Color", desc = "Color for the Invisibug highlight")
    @ConfigEditorColour
    @OnlyModern
    var color: ChromaColour = Color.CYAN.toChromaColor()
}
