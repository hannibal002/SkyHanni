package at.hannibal2.skyhanni.config.features.slayer.vampire

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class BossConfig {
    @Expose
    @ConfigOption(name = "Highlight Bosses", desc = "Highlight all Vampire Slayer bosses.")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlight: Boolean = true

    @Expose
    @ConfigOption(name = "Highlight Color", desc = "What color to highlight Vampire Slayer bosses in.")
    @ConfigEditorColour
    var highlightColor: ChromaColour = ChromaColour.fromStaticRGB(0, 255, 88, 249)

    @Expose
    @ConfigOption(name = "Steak Alert", desc = "Show a title when any boss can be steaked.")
    @ConfigEditorBoolean
    @FeatureToggle
    var steakAlert: Boolean = true

    @Expose
    @ConfigOption(name = "Twinclaws Title", desc = "Send a title when Twinclaws is about to happen.")
    @ConfigEditorBoolean
    @FeatureToggle
    var twinClawsTitle: Boolean = true
}
