package at.hannibal2.skyhanni.config.features.slayer.endermen

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class EndermanConfig {
    @Expose
    @ConfigOption(name = "Yang Glyph (Beacon)", desc = "")
    @Accordion
    val beacon: EndermanBeaconConfig = EndermanBeaconConfig()

    @Expose
    @ConfigOption(
        name = "Voidgloom in Dragon's Nest",
        desc = "Show all Enderman Slayer Features while in the Dragon's Nest.",
    )
    @ConfigEditorBoolean
    val showInNest: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Voidgloom Everywhere",
        desc = "Show all Enderman Slayer Features while outside of Void Sepulture and Zealot Bruiser Hideout.",
    )
    @ConfigEditorBoolean
    val showInNoArea: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Highlight Nukekubi Skulls", desc = "Highlight the Enderman Slayer Nukekubi Skulls (Eyes).")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightNukekebi: Boolean = false

    @Expose
    @ConfigOption(name = "Line to Nukekubi Skulls", desc = "Draw a line to the Enderman Slayer Nukekubi Skulls.")
    @ConfigEditorBoolean
    @FeatureToggle
    var drawLineToNukekebi: Boolean = false

    @Expose
    @ConfigOption(name = "Phase Display", desc = "Show the current phase of the Enderman Slayer.")
    @ConfigEditorBoolean
    var phaseDisplay: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Particles", desc = "Hide particles around Enderman Slayer bosses and Mini-Bosses.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideParticles: Boolean = false
}
