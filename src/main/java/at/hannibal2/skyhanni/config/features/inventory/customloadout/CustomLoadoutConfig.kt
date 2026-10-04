package at.hannibal2.skyhanni.config.features.inventory.customloadout

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.features.inventory.customwardrobe.CustomWardrobeConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CustomLoadoutConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Enable the Custom Loadout GUI.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Follow mouse", desc = "Whether the \"players\" follow the movement of the mouse.")
    @ConfigEditorBoolean
    var eyesFollowMouse: Boolean = true

    @Expose
    var onlyFavorites: Boolean = false

    @Expose
    @ConfigOption(name = "Colors", desc = "Change the color settings of the loadout, wardrobe and equipment GUIs.")
    @Accordion
    val color: ColorConfig = ColorConfig()

    @Expose
    @ConfigOption(name = "Spacing", desc = "Change the spacing settings of the loadout, wardrobe and equipment GUIs.")
    @Accordion
    val spacing: SpacingConfig = SpacingConfig()

    @Expose
    @ConfigOption(name = "Keybinds", desc = "")
    @Accordion
    val keybinds: LoadoutKeybindConfig = LoadoutKeybindConfig()

    @Expose
    @ConfigOption(name = "Highlighting", desc = "")
    @Accordion
    val highlighting: LoadoutHighlightingConfig = LoadoutHighlightingConfig()

    @Expose
    @ConfigOption(name = "Wardrobe & Equipment", desc = "")
    @Accordion
    val wardrobe: CustomWardrobeConfig = CustomWardrobeConfig()
}
