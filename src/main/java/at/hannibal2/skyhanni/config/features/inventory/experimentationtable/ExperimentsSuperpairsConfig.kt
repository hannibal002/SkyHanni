package at.hannibal2.skyhanni.config.features.inventory.experimentationtable

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import java.awt.Color

class ExperimentsSuperpairsConfig {

    class VisibilityConfig {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Keep clicked items visible to help create matches.")
        @ConfigEditorBoolean
        var enabled: Boolean = true

        @Expose
        @ConfigOption(name = "Highlight Items", desc = "")
        @Accordion
        val highlight: HighlightConfig = HighlightConfig()

        class HighlightConfig {
            @Expose
            @ConfigOption(
                name = "Enabled",
                desc = "Enable highlighting for items under certain conditions.\n" +
                    "Individual highlight types can be turned off by setting the color to transparent.",
            )
            @ConfigEditorBoolean
            var enabled: Boolean = true

            enum class HighlightType(val display: String) {
                BORDER("Border"),
                FULL("Full"),
                ;

                override fun toString(): String = display
            }

            // TODO add back once the option is getting used
//             @Expose
//             @ConfigOption(name = "Highlight Type", desc = "How the slot will be highlighted.")
//             @ConfigEditorDropdown
//             var type: HighlightType = HighlightType.FULL

            @Expose
            @ConfigOption(name = "Matched Pairs", desc = "Color for pairs you have already matched, and will receive in rewards.")
            @ConfigEditorColour
            var matchedPairColor: ChromaColour = Color.GREEN.toChromaColor(alpha = 150)

            @Expose
            @ConfigOption(name = "Uncovered Pairs", desc = "Color for pairs you have uncovered, but have not yet matched.")
            @ConfigEditorColour
            var uncoveredPairColor: ChromaColour = Color.YELLOW.toChromaColor(alpha = 150)

            @Expose
            @ConfigOption(name = "Second Click Pair", desc = "Color for an item that would complete a pair on your second click.")
            @ConfigEditorColour
            var secondClickPairColor: ChromaColour = Color.MAGENTA.toChromaColor(alpha = 150)
        }
    }

    @Expose
    @ConfigOption(name = "Keep Items Visible", desc = "")
    @Accordion
    val clickedItemsVisible: VisibilityConfig = VisibilityConfig()

    @Expose
    @ConfigOption(name = "Superpair Data", desc = "Displays useful data while doing the Superpair experiment.")
    @ConfigEditorBoolean
    @FeatureToggle
    var display: Boolean = false

    @Expose
    @ConfigLink(owner = ExperimentsSuperpairsConfig::class, field = "display")
    val displayPosition: Position = Position(-372, 161)

    @Expose
    @ConfigOption(name = "Superpairs XP Overlay", desc = "Shows how much XP every pair is worth in superpairs.")
    @ConfigEditorBoolean
    @FeatureToggle
    var xpOverlay: Boolean = true

    @Expose
    @ConfigOption(name = "ULTRA-RARE Book Alert", desc = "Send a chat message, title and sound when you find an ULTRA-RARE book.")
    @ConfigEditorBoolean
    @FeatureToggle
    var ultraRareBookAlert: Boolean = false

}
