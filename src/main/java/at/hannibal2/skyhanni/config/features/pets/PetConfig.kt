package at.hannibal2.skyhanni.config.features.pets

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.features.pets.display.PetDisplayConfig
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PetConfig {
    @Expose
    @ConfigOption(name = "Pet Display", desc = "")
    @Accordion
    val display: PetDisplayConfig = PetDisplayConfig()

    @Expose
    @ConfigOption(name = "Pet Experience Tooltip", desc = "")
    @Accordion
    val petExperienceToolTip: PetExperienceToolTipConfig = PetExperienceToolTipConfig()

    @Expose
    @ConfigOption(name = "Pet Nametag", desc = "")
    @Accordion
    val nametag: PetNametagConfig = PetNametagConfig()

    @Expose
    @ConfigOption(name = "Highlight Current Pet", desc = "")
    @Accordion
    val highlightInMenu: MenuHighlightConfig = MenuHighlightConfig()

    class MenuHighlightConfig {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Highlight your current pet in the §e/pets §7menu.")
        @ConfigEditorBoolean
        val enabled: Boolean = true

        @Expose
        @ConfigOption(name = "Highlight Color", desc = "What color the slot should be highlighted.")
        @ConfigEditorColour
        val color: ChromaColour = LorenzColor.GREEN.toChromaColor(alpha = 128)
    }

    @Expose
    @ConfigOption(
        name = "Hide Autopet Messages",
        desc = "Hide autopet messages in chat."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideAutopet: Boolean = false
}
