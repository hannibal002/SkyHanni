package at.hannibal2.skyhanni.config.features.event.winter

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FrozenTreasureHighlighterConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Highlights Frozen Treasures in the Glacial Cave on the Jerry Island.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Highlight Color",
        desc = "The color that frozen treasures should be highlighted in.",
    )
    @ConfigEditorColour
    var treasureColor: ChromaColour = LorenzColor.GREEN.toChromaColor(191)

}
