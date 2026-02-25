package at.hannibal2.skyhanni.config.features.gui

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HotbarConfig {
    @Expose
    @ConfigOption(
        name = "Editable",
        desc = "Add the hotbar to the gui editor. Allows for moving and scaling of the hotbar."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var editable: Boolean = false

    @Expose
    @ConfigLink(owner = HotbarConfig::class, field = "editable")
    val hotbar: Position = Position(20, 20)

    @Expose
    @ConfigOption(name = "Show Outside SkyBlock", desc = "Enable the hotbar to be edited even outside of SkyBlock.")
    @ConfigEditorBoolean
    var showOutsideSkyblock: Boolean = false
}
