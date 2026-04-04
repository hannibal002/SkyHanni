package at.hannibal2.skyhanni.config.features.pets.display

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.pets.display.text.TextPetDisplayConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.ExpSharePetDisplayConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.VisualPetDisplayConfig
import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.observer.Property

class PetDisplayConfig : Config() {
    override fun saveNow() = Unit

    override fun getTitle(): StructuredText = "Pet Display".asStructuredText()

    @Expose
    @Category(name = "General", desc = "General settings for the pet display.")
    val general: GeneralPetDisplayConfig = GeneralPetDisplayConfig()

    class GeneralPetDisplayConfig {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show a GUI element for the currently active pet.")
        @ConfigEditorBoolean
        @FeatureToggle
        val enabled: Property<Boolean> = Property.of(false)

        @Expose
        @ConfigLink(owner = GeneralPetDisplayConfig::class, field = "enabled")
        val position: Position = Position(200, 200)
    }

    @Expose
    @Category(name = "Visual Elements", desc = "Visual element settings for the pet display.")
    val visual: MainVisualPetDisplayConfig = MainVisualPetDisplayConfig()

    class MainVisualPetDisplayConfig : VisualPetDisplayConfig() {
        @Expose
        @ConfigOption(name = "Exp-Share Pets Customization", desc = "")
        @Accordion
        val expSharePets: ExpSharePetDisplayConfig = ExpSharePetDisplayConfig(scalar = 0.6f)
    }

    @Expose
    @Category(name = "Text Elements", desc = "Text element settings for the pet display.")
    val text: TextPetDisplayConfig = TextPetDisplayConfig()
}
