package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class QuickModMenuSwitchConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Add a mod list, allowing quick switching between different mod menus.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Inside Escape Menu", desc = "Show the mod list while inside the Escape menu.")
    @ConfigEditorBoolean
    var insideEscapeMenu: Boolean = true

    @Expose
    @ConfigOption(
        name = "Inside Inventory",
        desc = "Show the mod list while inside the player inventory (no chest inventory)."
    )
    @ConfigEditorBoolean
    var insidePlayerInventory: Boolean = false

    @Expose
    @ConfigLink(owner = QuickModMenuSwitchConfig::class, field = "enabled")
    val pos: Position = Position(-178, 143)
}
