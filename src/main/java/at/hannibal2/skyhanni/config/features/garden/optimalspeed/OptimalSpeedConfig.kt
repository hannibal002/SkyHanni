package at.hannibal2.skyhanni.config.features.garden.optimalspeed

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class OptimalSpeedConfig {
    @Expose
    @ConfigOption(
        name = "Show on HUD",
        desc = "Show the optimal speed for your current tool in the hand.\n" +
            "(Thanks §bMelonKingDE §7for the default values)."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var showOnHUD: Boolean = false

    @Expose
    @ConfigOption(
        name = "Wrong Speed Warning",
        desc = "Warn via title and chat message when you don't have the optimal speed."
    )
    @ConfigEditorBoolean
    var warning: Boolean = false

    @Expose
    @ConfigOption(name = "Only Warn With Rancher's", desc = "Only send a warning when wearing Rancher's Boots.")
    @ConfigEditorBoolean
    var onlyWarnRanchers: Boolean = false

    @Expose
    @ConfigOption(
        name = "Rancher Boots",
        desc = "Set the optimal speed in the Rancher Boots overlay by clicking on the presets."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var signEnabled: Boolean = true

    @Expose
    @ConfigOption(name = "Compact GUI", desc = "Compact the Rancher Boots GUI only showing crop icons")
    @ConfigEditorBoolean
    var compactRancherGui: Boolean = false

    @Expose
    @ConfigLink(owner = OptimalSpeedConfig::class, field = "signEnabled")
    val signPosition: Position = Position(20, -195)

    @Expose
    @ConfigOption(name = "Custom Speed", desc = "Change the exact speed for every single crop.")
    @Accordion
    val customSpeed: CustomSpeedConfig = CustomSpeedConfig()

    @Expose
    @ConfigLink(owner = OptimalSpeedConfig::class, field = "showOnHUD")
    val pos: Position = Position(5, -200)
}
