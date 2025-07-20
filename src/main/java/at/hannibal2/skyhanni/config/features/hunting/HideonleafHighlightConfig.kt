package at.hannibal2.skyhanni.config.features.hunting

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

class HideonleafHighlightConfig {

    @Expose
    @ConfigOption(name = "Highlight Hideonleaf", desc = "Highlights nearby Hideonleaf.")
    @FeatureToggle
    @ConfigEditorBoolean
    @OnlyModern
    @SearchTag("shulker")
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Color", desc = "Color for the Hideonleaf highlight")
    @ConfigEditorColour
    @OnlyModern
    @SearchTag("shulker")
    var color: ChromaColour = Color.MAGENTA.toChromaColor()

}
