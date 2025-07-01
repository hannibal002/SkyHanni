package at.hannibal2.skyhanni.config.features.event

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class AnniversaryTeamFinderColorConfig {

    @Expose
    @ConfigOption(name = "Wrong Color", desc = "Color for the wrong team players.")
    @ConfigEditorColour
    val wrong: Property<String> = Property.of("0:1:76:76:76")

    @Expose
    @ConfigOption(name = "Pink Color", desc = "Color for the team §dPINK §7players.")
    @ConfigEditorColour
    val pink: Property<String> = Property.of("0:1:241:80:241")

    @Expose
    @ConfigOption(name = "Blue Color", desc = "Color for the team §9BLUE §7players.")
    @ConfigEditorColour
    val blue: Property<String> = Property.of("0:1:85:85:255")

    @Expose
    @ConfigOption(name = "Yellow Color", desc = "Color for the team §eYELLOW §7players.")
    @ConfigEditorColour
    val yellow: Property<String> = Property.of("0:1:255:255:85")

    @Expose
    @ConfigOption(name = "Green Color", desc = "Color for the team §aGREEN §7players.")
    @ConfigEditorColour
    val green: Property<String> = Property.of("0:1:85:255:85")

    @Expose
    @ConfigOption(name = "Red Color", desc = "Color for the team §ARED §7players.")
    @ConfigEditorColour
    val red: Property<String> = Property.of("0:1:255:85:85")
}
