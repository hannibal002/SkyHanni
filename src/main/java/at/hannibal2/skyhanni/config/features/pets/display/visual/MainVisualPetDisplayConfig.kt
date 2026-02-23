package at.hannibal2.skyhanni.config.features.pets.display.visual

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MainVisualPetDisplayConfig : VisualPetDisplayConfig() {
    @Expose
    @ConfigOption(name = "Exp-Share Pets Customization", desc = "")
    @Accordion
    val expSharePets: ExpSharePetDisplayConfig = ExpSharePetDisplayConfig()
}
