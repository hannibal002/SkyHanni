package at.hannibal2.skyhanni.config.features.hunting

import at.hannibal2.skyhanni.config.ConfigEditorKeyMapping
import at.hannibal2.skyhanni.config.ConfigKeybind
import at.hannibal2.skyhanni.utils.InputCode
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import io.github.notenoughupdates.moulconfig.observer.Property

class FusionKeybindsConfig {

    @ConfigOption(
        name = "§cNotice",
        desc = "The keybinds below must be different, and won't work if you hold both at the same time.",
    )
    @SearchTag("fusion hunting box")
    @ConfigEditorInfoText
    val notice: String = ""

    @Expose
    @ConfigOption(name = "Repeat Fusion Keybind", desc = "Keybind to repeat the previous fusion.")
    @SearchTag("hunting box")
    @ConfigEditorKeyMapping
    val repeatFusionKeybind: Property<ConfigKeybind> = Property.of(ConfigKeybind(InputCode.UNKNOWN))

    @Expose
    @ConfigOption(name = "Confirm Fusion Keybind", desc = "Keybind to confirm the current fusion.")
    @SearchTag("hunting box")
    @ConfigEditorKeyMapping
    val confirmFusionKeybind: Property<ConfigKeybind> = Property.of(ConfigKeybind(InputCode.UNKNOWN))
}
