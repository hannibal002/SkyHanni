package at.hannibal2.skyhanni.config.features.misc.pets

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import java.util.*

class PetConfig {
    @Expose
    @ConfigOption(name = "Pet Display", desc = "Show the currently active pet.")
    @ConfigEditorBoolean
    @FeatureToggle
    var display: Boolean = false

    @Expose
    @ConfigLink(owner = PetConfig::class, field = "display")
    var displayPos: Position = Position(-330, -15)

    @Expose
    @ConfigOption(name = "Pet Experience Tooltip", desc = "")
    @Accordion
    var petExperienceToolTip: PetExperienceToolTipConfig = PetExperienceToolTipConfig()

    @Expose
    @ConfigOption(name = "Pet Nametag", desc = "")
    @Accordion
    var nametag: PetNametagConfig = PetNametagConfig()

    @Expose
    @ConfigOption(
        name = "Hide Autopet Messages",
        desc = "Hide the autopet messages from chat.\n" +
            "§eRequires the display to be enabled."
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
    var petItemDisplay: MutableList<PetItemsDisplay> = mutableListOf(
        PetItemsDisplay.XP_SHARE,
        PetItemsDisplay.TIER_BOOST
    )

    @Expose
    @ConfigOption(name = "Pet Item Scale", desc = "The scale at which the Pet Item will be displayed.")
    @ConfigEditorSlider(minValue = 0.7f, maxValue = 1.5f, minStep = 0.05f)
    var petItemDisplayScale: Float = 0.9f


    enum class PetItemsDisplay(val icon: String, private val displayName: String, val item: String) {
        XP_SHARE("§5⚘", "Exp Share", "PET_ITEM_EXP_SHARE"),
        TIER_BOOST("§c●", "Tier Boost", "PET_ITEM_TIER_BOOST"),
        ;

        override fun toString() = "$icon §ffor $displayName"
    }
}
