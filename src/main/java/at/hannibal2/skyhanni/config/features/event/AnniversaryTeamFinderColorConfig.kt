package at.hannibal2.skyhanni.config.features.event

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class AnniversaryTeamFinderColorConfig {

    @Expose
    @ConfigOption(name = "Wrong Color", desc = "Color for the wrong team players.")
    @ConfigEditorColour
    val wrong: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(76, 76, 76, 1))

    @Expose
    @ConfigOption(name = "Pink Color", desc = "Color for the team §dPINK §7players.")
    @ConfigEditorColour
    val pink: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(241, 80, 241, 1))

    @Expose
    @ConfigOption(name = "Blue Color", desc = "Color for the team §9BLUE §7players.")
    @ConfigEditorColour
    val blue: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(85, 85, 255, 1))

    @Expose
    @ConfigOption(name = "Yellow Color", desc = "Color for the team §eYELLOW §7players.")
    @ConfigEditorColour
    val yellow: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(255, 255, 85, 1))

    @Expose
    @ConfigOption(name = "Green Color", desc = "Color for the team §aGREEN §7players.")
    @ConfigEditorColour
    val green: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(85, 255, 85, 1))

    @Expose
    @ConfigOption(name = "Red Color", desc = "Color for the team §ARED §7players.")
    @ConfigEditorColour
    val red: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(255, 85, 85, 1))
}
