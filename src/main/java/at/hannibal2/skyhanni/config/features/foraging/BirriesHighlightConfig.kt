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

class BirriesHighlightConfig {

    @Expose
    @ConfigOption(name = "Highlight Birries", desc = "Highlights nearby Birries.")
    @FeatureToggle
    @ConfigEditorBoolean
    @OnlyModern
    @SearchTag("tadpole tad pole poll")
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Color", desc = "Color for the Birries highlight")
    @ConfigEditorColour
    @OnlyModern
    @SearchTag("tadpole tad pole poll")
    var color: ChromaColour = Color.GREEN.toChromaColor()

}
