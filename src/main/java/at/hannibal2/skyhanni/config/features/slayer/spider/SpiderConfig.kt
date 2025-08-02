package at.hannibal2.skyhanni.config.features.slayer.spider

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class SpiderConfig {

    @Expose
    @ConfigOption(name = "Mark When Invincible", desc = "Highlight the Tarantula Slayer tier 5 when the hatchlings are alive.")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightInvincible: Boolean = true

    @Expose
    @ConfigOption(name = "Invincible Color", desc = "The color used to highlight the invincible phase.")
    @ConfigEditorColour
    val highlightInvincibleColor: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(255, 255, 0, 60))

}
