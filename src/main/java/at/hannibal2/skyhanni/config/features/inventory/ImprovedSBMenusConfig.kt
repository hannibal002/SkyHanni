package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.OnlyModern
import at.hannibal2.skyhanni.features.inventory.LegacyBetterContainers
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ImprovedSBMenusConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Change the way that menus in SkyBlock look.")
    @ConfigEditorBoolean
    @FeatureToggle
    @OnlyModern
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Menu Background Style",
        desc = "Change the background style of SkyBlock menus."
    )
    @ConfigEditorDropdown
    @OnlyModern
    var menuBackgroundStyle: LegacyBetterContainers.BackgroundStyle = LegacyBetterContainers.BackgroundStyle.DARK_1

    @Expose
    @ConfigOption(
        name = "Button Background Style",
        desc = "Change the background style of foreground elements in SkyBlock menus."
    )
    @ConfigEditorDropdown
    @OnlyModern
    var buttonBackgroundStyle: LegacyBetterContainers.BackgroundStyle = LegacyBetterContainers.BackgroundStyle.DARK_1

}
