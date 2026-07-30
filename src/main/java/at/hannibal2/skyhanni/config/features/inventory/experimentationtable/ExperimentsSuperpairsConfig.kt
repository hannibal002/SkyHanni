package at.hannibal2.skyhanni.config.features.inventory.experimentationtable

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ExperimentsSuperpairsConfig {

    class VisibilityConfig {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Keep clicked items visible to help create matches.")
        @ConfigEditorBoolean
        @FeatureToggle
        var enabled: Boolean = true
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
    @ConfigOption(name = "Highlight Pairs", desc = "Highlight collected pairs, known matches and cards seen once in the grid.")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightPairs: Boolean = true

    @Expose
    @ConfigOption(name = "Collected Color", desc = "Color for pairs you have already collected.")
    @ConfigEditorColour
    var collectedColor: ChromaColour = LorenzColor.GREEN.toChromaColor(128)

    @Expose
    @ConfigOption(name = "Matched Color", desc = "Color for known matches you haven't collected yet.")
    @ConfigEditorColour
    var matchedColor: ChromaColour = LorenzColor.GOLD.toChromaColor(128)

    @Expose
    @ConfigOption(name = "Seen Color", desc = "Color for cards revealed once whose partner is still unknown.")
    @ConfigEditorColour
    var seenColor: ChromaColour = LorenzColor.YELLOW.toChromaColor(128)

    @Expose
    @ConfigOption(name = "Powerup Color", desc = "Color for uncovered powerups.")
    @ConfigEditorColour
    var powerupColor: ChromaColour = LorenzColor.DARK_PURPLE.toChromaColor(128)

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
