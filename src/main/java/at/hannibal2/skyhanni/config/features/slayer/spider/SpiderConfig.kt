package at.hannibal2.skyhanni.config.features.slayer.spider

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import io.github.notenoughupdates.moulconfig.observer.Property

class SpiderConfig {

    @Expose
    @ConfigOption(name = "Mark When Invincible", desc = "Highlight the Tarantula Slayer tier 5 when the hatchlings are alive.")
    @SearchTag("Spider")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightInvincible: Boolean = true

    @Expose
    @ConfigOption(name = "Invincible Color", desc = "The color used to highlight the invincible phase.")
    @ConfigEditorColour
    val highlightInvincibleColor: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(255, 255, 0, 60))

    @Expose
    @ConfigOption(name = "Phase Display", desc = "Show the current phase of the Tara 5 Slayer boss.")
    @ConfigEditorBoolean
    var phaseDisplay: Boolean = false

    @Expose
    @ConfigOption(name = "Line to Tarantula Boss", desc = "Adds a line to your Tarantula Broodfather Boss.")
    @SearchTag("Spider")
    @ConfigEditorBoolean
    @FeatureToggle
    var lineToBoss: Boolean = false

    @Expose
    @ConfigOption(
        name = "Line to Tarantula Width",
        desc = "The width of the line pointing to your Tarantula Broodfather.",
    )
    @SearchTag("Spider")
    @ConfigEditorSlider(minStep = 1f, minValue = 1f, maxValue = 10f)
    var slayerLineWidth: Int = 3

    @Expose
    @ConfigOption(
        name = "Highlight Egg Sacs",
        desc = "Highlight the Egg Sacs spawned by the Tarantula Broodfather.",
    )
    @SearchTag("Spider")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightEggSacs: Boolean = true

    @Expose
    @ConfigOption(
        name = "Egg Sac Color",
        desc = "Color used to highlight Tarantula Broodfather Egg Sacs.",
    )
    @SearchTag("Spider")
    @ConfigEditorColour
    var eggSacHighlightColor: ChromaColour = ChromaColour.fromStaticRGB(255, 255, 0, 120)
}
