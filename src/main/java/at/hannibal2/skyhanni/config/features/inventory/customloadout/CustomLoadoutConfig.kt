package at.hannibal2.skyhanni.config.features.inventory.customloadout

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CustomLoadoutConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Enable the Custom Loadout GUI.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Follow mouse", desc = "Whether the \"players\" follow the movement of the mouse.")
    @ConfigEditorBoolean
    var eyesFollowMouse: Boolean = true

    @Expose
    var onlyFavorites: Boolean = false

    @Expose
    @ConfigOption(name = "Colors", desc = "Change the color settings.")
    @Accordion
    val color: LoadoutColorConfig = LoadoutColorConfig()

    @Expose
    @ConfigOption(name = "Spacing", desc = "")
    @Accordion
    val spacing: LoadoutSpacingConfig = LoadoutSpacingConfig()

    @Expose
    @ConfigOption(name = "Keybinds", desc = "")
    @Accordion
    val keybinds: LoadoutKeybindConfig = LoadoutKeybindConfig()

    @Expose
    @ConfigOption(name = "Highlighting", desc = "")
    @Accordion
    val highlighting: LoadoutHighlightingConfig = LoadoutHighlightingConfig()
}
