package at.hannibal2.skyhanni.config.features.itemability

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FireVeilWandConfig {
    @Expose
    @ConfigOption(name = "Fire Veil Design", desc = "Change the flame particles of the Fire Veil Wand ability.")
    @ConfigEditorDropdown
    var display: DisplayEntry = DisplayEntry.PARTICLES

    enum class DisplayEntry(private val displayName: String) {
        PARTICLES("Particles"),
        LINE("Line"),
        OFF("Off"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Line Color", desc = "Change the color of the Fire Veil Wand line.")
    @ConfigEditorColour
    var displayColor: String = "0:245:255:85:85"
}
