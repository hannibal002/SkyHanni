package at.hannibal2.skyhanni.config.features.garden.leaderboards.generics

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

open class GardenDisplayGenericConfig : EliteDisplayGenericConfig() {

    @Expose
    @ConfigOption(name = "Show Outside Garden", desc = "Show this display outside of the garden.")
    @ConfigEditorBoolean
    var showOutsideGarden: Boolean = false
}

