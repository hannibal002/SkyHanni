package at.hannibal2.skyhanni.config.features.pets

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PetConfig {
    @Expose
    @ConfigOption(name = "Pet Display", desc = "Show the currently active pet.")
    @ConfigEditorBoolean
    @FeatureToggle
    var display: Boolean = false

    @Expose
    @ConfigLink(owner = PetConfig::class, field = "display")
    val displayPos: Position = Position(-330, -15)

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
        var enabled: Boolean = true

        @Expose
        @ConfigOption(name = "Highlight Color", desc = "What color the slot should be highlighted.")
        @ConfigEditorColour
        var color: ChromaColour = LorenzColor.GREEN.toChromaColor(alpha = 128)
    }

    @Expose
    @ConfigOption(
        name = "Hide Autopet Messages",
        desc = "Hide autopet messages in chat."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideAutopet: Boolean = false

    @Expose
    @ConfigOption(
        name = "Show Pet Item",
        desc = "Specify the pet items for which icons should be displayed next to pets."
    )
    @ConfigEditorDraggableList
    val petItemDisplay: MutableList<PetItemsDisplay> = mutableListOf(
        PetItemsDisplay.XP_SHARE,
        PetItemsDisplay.TIER_BOOST
    )

    @Expose
    @ConfigOption(name = "Pet Item Scale", desc = "The scale at which the Pet Item will be displayed.")
    @ConfigEditorSlider(minValue = 0.7f, maxValue = 1.5f, minStep = 0.05f)
    var petItemDisplayScale: Float = 0.9f


    enum class PetItemsDisplay(
        val icon: String,
        itemDisplayName: String,
        val item: String
    ) {
        XP_SHARE("§5⚘", "Exp Share", "PET_ITEM_EXP_SHARE"),
        TIER_BOOST("§c●", "Tier Boost", "PET_ITEM_TIER_BOOST"),
        ;

        private val displayName: String = "$icon §ffor $itemDisplayName"

        override fun toString() = displayName
    }
}
