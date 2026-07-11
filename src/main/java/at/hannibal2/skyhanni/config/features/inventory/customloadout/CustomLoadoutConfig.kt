package at.hannibal2.skyhanni.config.features.inventory.customloadout

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CustomLoadoutConfig {

    @Expose
    @ConfigOption(name = "Keybinds", desc = "")
    @Accordion
    val keybinds: LoadoutKeybindConfig = LoadoutKeybindConfig()
}
