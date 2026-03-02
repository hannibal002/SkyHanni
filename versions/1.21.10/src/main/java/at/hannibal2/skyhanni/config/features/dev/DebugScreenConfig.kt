package at.hannibal2.skyhanni.config.features.dev

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DebugScreenConfig {
    @ConfigOption(
        name = "§cOptions Moved",
        desc = "§eThese options are configured in F3+F6 in your Minecraft version (search for \"skyhanni\")."
    )
    @ConfigEditorInfoText
    var movedWarning: String = ""
}
