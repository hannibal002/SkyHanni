package at.hannibal2.skyhanni.config.features.dungeon.spiritleap

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SpiritLeapConfig {
    @Expose
    @ConfigOption(name = "Enable Spirit Leap Overlay", desc = "Enable Spirit Leap Overlay inside Dungeons.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Show Class Level",
        desc = "Display the player's Class level in the Spirit Leap overlay.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var showDungeonClassLevel: Boolean = false

    @Expose
    @ConfigOption(
        name = "Spirit Leap Colors",
        desc = "Configure colors for each class and dead teammates.",
    )
    @Accordion
    var colorConfig = SpiritLeapColorConfig()

    @Expose
    @ConfigOption(
        name = "Spirit Leap Keybinds",
        desc = "Set keybinds and show keybind hints for Spirit Leap."
    )
    @Accordion
    var spiritLeapKeybindConfig = SpiritLeapKeybindConfig()
}
