package at.hannibal2.skyhanni.config.features.inventory.helper

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HelperConfig {
    @Expose
    @ConfigOption(name = "Melody's Hair Harp", desc = "")
    @Accordion
    val harp: HarpConfig = HarpConfig()

    class HarpConfig {
        @Expose
        @ConfigOption(name = "GUI Scale", desc = "Automatically set the GUI scale to \"AUTO\" when entering the Harp.")
        @ConfigEditorBoolean
        var guiScale: Boolean = false

        @Expose
        @ConfigOption(
            name = "Quick Restart",
            desc = "Once you've launched the Harp, quickly hit the close button in the Harp Menu to initiate the selected song."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        var quickRestart: Boolean = false

        @Expose
        @ConfigOption(
            name = "Use Keybinds",
            desc = "In the Harp, press buttons with your number row on the keyboard instead of clicking."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        var keybinds: Boolean = false

        @Expose
        @ConfigOption(
            name = "Show Numbers",
            desc = "In the Harp, show buttons as stack size (intended to be used with the Keybinds)."
        )
        @ConfigEditorBoolean
        var showNumbers: Boolean = false

        @Expose
        @ConfigOption(name = "Hide Tooltip", desc = "Hide the item tooltips inside the Harp.")
        @ConfigEditorBoolean
        @FeatureToggle
        var hideMelodyTooltip: Boolean = false

        @Expose
        @ConfigOption(name = "Keybinds", desc = "")
        @Accordion
        val harpKeybinds: HarpConfigKeyBinds = HarpConfigKeyBinds()
    }

    @Expose
    @ConfigOption(name = "Tia Relay Abiphone Network Maintenance", desc = "")
    @Accordion
    val tiaRelay: TiaRelayConfig = TiaRelayConfig()

    @Expose
    @ConfigOption(name = "Reforge Helper", desc = "")
    @Accordion
    val reforge: ReforgeHelperConfig = ReforgeHelperConfig()
}
