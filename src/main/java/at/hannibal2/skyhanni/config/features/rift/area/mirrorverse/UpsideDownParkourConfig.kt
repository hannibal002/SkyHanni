package at.hannibal2.skyhanni.config.features.rift.area.mirrorverse

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class UpsideDownParkourConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Help solve the upside down parkour in the Mirrorverse by showing the correct way."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Look Ahead", desc = "Change how many platforms should be shown in front of you.")
    @ConfigEditorSlider(minStep = 1f, maxValue = 9f, minValue = 1f)
    val lookAhead: Property<Int> = Property.of(3)

    @Expose
    @ConfigOption(name = "Outline", desc = "Outline the top edge of the platforms.")
    @ConfigEditorBoolean
    var outline: Boolean = true

    @Expose
    @ConfigOption(name = "Rainbow Color", desc = "Show the rainbow color effect instead of a boring monochrome.")
    @ConfigEditorBoolean
    val rainbowColor: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Monochrome Color", desc = "Set a boring monochrome color for the parkour platforms.")
    @ConfigEditorColour
    val monochromeColor: Property<String> = Property.of("0:60:0:0:255")

    @Expose
    @ConfigOption(name = "Hide Others Players", desc = "Hide other players while doing the upside down parkour.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hidePlayers: Boolean = false
}
