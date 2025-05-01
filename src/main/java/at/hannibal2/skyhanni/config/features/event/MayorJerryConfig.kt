package at.hannibal2.skyhanni.config.features.event

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class MayorJerryConfig {
    @Expose
    @ConfigOption(
        name = "Highlight Jerries",
        desc = "Highlight Jerries found from the Jerrypocalypse perk. Highlight color is based on color of the Jerry."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightJerries: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Line to Jerries", desc = "Shows a line to your Jerries found from the Jerrypocalypse perk.")
    @ConfigEditorBoolean
    @FeatureToggle
    var lineJerries: Property<Boolean> = Property.of(true)
}
