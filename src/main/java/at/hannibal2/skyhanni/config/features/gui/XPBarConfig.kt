package at.hannibal2.skyhanni.config.features.gui

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.OnlyLegacy
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class XPBarConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Allows for moving and scaling the XP bar in the SkyHanni GUI Editor.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @ConfigOption(
        name = "§cNotice",
        desc = "This option will be §c§lincompatible §r§7with mods that change the xp bar. Eg: §eApec§7."
    )
    @OnlyLegacy
    @ConfigEditorInfoText
    var notice: String = ""

    @Expose
    @ConfigLink(owner = XPBarConfig::class, field = "enabled")
    val position: Position = Position(20, 20)

    @Expose
    @ConfigOption(name = "Show Outside Skyblock", desc = "Shows the XP bar outside of SkyBlock.")
    @ConfigEditorBoolean
    var showOutsideSkyblock: Boolean = false
}
