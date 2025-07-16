package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class GlowingMushroomHighlighterConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Highlights Glowing Mushrooms in the glowing mushroom cave.")
    @ConfigEditorBoolean
    @SearchTag("moby chum")
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Highlight Color",
        desc = "The color that glowing mushrooms should be highlighted in.",
    )
    @ConfigEditorColour
    var mushroomColor: ChromaColour = LorenzColor.AQUA.toChromaColor(127)

}
