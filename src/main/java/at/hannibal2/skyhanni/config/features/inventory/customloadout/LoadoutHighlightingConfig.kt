package at.hannibal2.skyhanni.config.features.inventory.customloadout

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class LoadoutHighlightingConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable highlighting of loadouts in the inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Currently Equipped", desc = "Highlight the currently equipped loadout in the inventory.")
    @ConfigEditorBoolean
    var currentlyEquipped: Boolean = true

    @Expose
    @ConfigOption(name = "Currently Equipped Color", desc = "The color used to highlight the currently equipped loadout in the inventory.")
    @ConfigEditorColour
    var equippedColor: ChromaColour = LorenzColor.GREEN.toChromaColor()

    @Expose
    @ConfigOption(name = "Favorites", desc = "Highlight favorite loadouts in the inventory.")
    @ConfigEditorBoolean
    var favorites: Boolean = false

    @Expose
    @ConfigOption(name = "Favorite Color", desc = "The color used to highlight favorite loadouts in the inventory.")
    @ConfigEditorColour
    var favoriteColor: ChromaColour = LorenzColor.YELLOW.toChromaColor()
}
