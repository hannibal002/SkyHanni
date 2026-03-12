package at.hannibal2.skyhanni.config.features.slayer

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class ZombieConfig {
    @Expose
    @ConfigOption(
        name = "Revenant In Graveyard",
        desc = "Show all Revenant Slayer Features while inside the Graveyard.",
    )
    @ConfigEditorBoolean
    val showInGraveyard: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Boom Display", desc = "Show BOOM for Revenant 5 when the boss is about to explode.")
    @ConfigEditorBoolean
    var boomDisplay: Boolean = false
}
