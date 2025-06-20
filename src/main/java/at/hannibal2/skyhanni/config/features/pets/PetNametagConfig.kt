package at.hannibal2.skyhanni.config.features.pets

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PetNametagConfig {
    @Expose
    @ConfigOption(name = "Hide Pet Level", desc = "Hide the pet level above the pet.")
    @ConfigEditorBoolean
    var hidePetLevel: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Max Pet Level", desc = "Hide the pet level above the pet if it is max level.")
    @ConfigEditorBoolean
    var hideMaxPetLevel: Boolean = false
}
