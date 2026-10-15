package at.hannibal2.skyhanni.config.features.slayer.vampire

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.features.slayer.VampireSlayerFeatures
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class KillerSpringConfig : VampireSlayerFeatures.VampireEffectConfig {
    @Expose
    @ConfigOption(name = "Highlight Killer Spring", desc = "Highlight the Killer Spring tower.")
    @ConfigEditorBoolean
    @FeatureToggle
    override var highlight: Boolean = false

    @Expose
    @ConfigOption(name = "Beacon Beam", desc = "Render a beacon beam where the Killer Spring is.")
    @ConfigEditorBoolean
    @FeatureToggle
    override var renderBeam: Boolean = false

    @Expose
    @ConfigOption(name = "Color", desc = "Highlight color.")
    @ConfigEditorColour
    override var color: ChromaColour = ChromaColour.fromStaticRGB(100, 0, 88, 199)

    @Expose
    @ConfigOption(
        name = "Show Lines",
        desc = "Draw lines that start from the head of the boss and end on the Killer Spring tower."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    override var showLines: Boolean = false

    @Expose
    @ConfigOption(name = "Lines Start Color", desc = "Starting color of the lines.")
    @ConfigEditorColour
    override var linesColor: ChromaColour = ChromaColour.fromStaticRGB(255, 13, 0, 255)

    @Expose
    @ConfigOption(
        name = "Fix Sound Spam",
        desc = "Fixes a Hypixel bug that overloads Minecraft's sound engine with Killer Spring sounds " +
            "and results in no sounds for a few seconds.",
    )
    @SearchTag("mute wither")
    @ConfigEditorBoolean
    @FeatureToggle
    var fixSoundSpam: Boolean = true
}
