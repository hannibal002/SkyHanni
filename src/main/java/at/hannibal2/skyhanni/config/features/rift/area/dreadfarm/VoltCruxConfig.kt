package at.hannibal2.skyhanni.config.features.rift.area.dreadfarm

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class VoltCruxConfig {
    @Expose
    @ConfigOption(name = "Volt Warning", desc = "Show a warning while a Volt is discharging lightning.")
    @ConfigEditorBoolean
    @FeatureToggle
    var voltWarning: Boolean = true

    @Expose
    @ConfigOption(name = "Volt Range Highlighter", desc = "Show the area in which a Volt might strike lightning.")
    @ConfigEditorBoolean
    @FeatureToggle
    var voltRange: Boolean = true

    @Expose
    @ConfigOption(name = "Volt Range Highlighter Color", desc = "In which color should the Volt range be highlighted?")
    @ConfigEditorColour
    var voltColor: ChromaColour = ChromaColour.fromStaticRGB(0, 0, 255, 60)

    @Expose
    @ConfigOption(name = "Volt Mood Color", desc = "Change the color of the Volt enemy depending on their mood.")
    @ConfigEditorBoolean
    @FeatureToggle
    var voltMoodMeter: Boolean = false
}
