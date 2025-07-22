package at.hannibal2.skyhanni.config.features.rift.area.livingcave

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class DefenseBlockConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show a line between Defense blocks and the mob and highlight the blocks.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Hide Particles", desc = "Hide particles around Defense Blocks.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideParticles: Boolean = false

    @Expose
    @ConfigOption(name = "Color", desc = "Set the color of the lines, blocks and the entity.")
    @ConfigEditorColour
    val color: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(77, 104, 255, 255))
}
