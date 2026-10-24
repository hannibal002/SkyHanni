package at.hannibal2.skyhanni.config.features.combat

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.features.combat.mobs.BestiaryMobHighlight
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class BestiaryMobHighlightsConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Middle-click a Bestiary entry to highlight the corresponding mob in the world.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val enabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Highlight Color",
        desc = "The color used for highlighted mobs and Bestiary entries.",
    )
    @ConfigEditorColour
    val highlightColor: Property<ChromaColour> =
        Property.of(ChromaColour.fromStaticRGB(255, 255, 0, 255))

    @Expose
    val highlights = mutableSetOf<BestiaryMobHighlight>()
}
