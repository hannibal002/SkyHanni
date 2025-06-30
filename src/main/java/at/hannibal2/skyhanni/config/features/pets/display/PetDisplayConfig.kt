package at.hannibal2.skyhanni.config.features.pets.display

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.pets.display.text.TextPetDisplayConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.VisualPetDisplayConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class PetDisplayConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show a GUI element for the currently active pet.")
    @ConfigEditorBoolean
    @FeatureToggle
    val enabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigLink(owner = PetDisplayConfig::class, field = "enabled")
    val position: Position = Position(200, 200)

    @Expose
    @ConfigOption(name = "Visual Elements", desc = "")
    @Accordion
    val visual: VisualPetDisplayConfig = VisualPetDisplayConfig()

    @Expose
    @ConfigOption(name = "Text Elements", desc = "")
    @Accordion
    val text: TextPetDisplayConfig = TextPetDisplayConfig()
}
