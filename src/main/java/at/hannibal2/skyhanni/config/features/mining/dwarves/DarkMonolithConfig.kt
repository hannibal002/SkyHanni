package at.hannibal2.skyhanni.config.features.mining.dwarves

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.IndividualItemTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DarkMonolithConfig {

    @Expose
    @ConfigOption(
        name = "Tracker",
        desc = "Track mithril powder, coins, and Rock the Fish drops obtained from collecting Dark Monoliths.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var tracker: Boolean = false

    @Expose
    @ConfigLink(owner = DarkMonolithConfig::class, field = "tracker")
    val trackerPosition: Position = Position(100, 100)

    @Expose
    @ConfigOption(name = "Beacon", desc = "")
    @Accordion
    val beacon: BeaconConfig = BeaconConfig()

    class BeaconConfig {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show a beacon at the location of the Dark Monolith.")
        @ConfigEditorBoolean
        @FeatureToggle
        var enabled: Boolean = false

        @Expose
        @ConfigOption(name = "Beacon Color", desc = "What color to show the beacon.\n§cCustom alpha values won't work§7.")
        @ConfigEditorColour
        var color: ChromaColour = ChromaColour.fromStaticRGB(155, 29, 194, 255)
    }

    @Expose
    @ConfigOption(name = "Highlight", desc = "")
    @Accordion
    val highlight: HighlightConfig = HighlightConfig()

    class HighlightConfig {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Highlight the dragon egg when in line of sight.")
        @ConfigEditorBoolean
        @FeatureToggle
        var enabled: Boolean = false

        @Expose
        @ConfigOption(name = "Highlight Color", desc = "What color to highlight the egg.")
        @ConfigEditorColour
        var color: ChromaColour = ChromaColour.fromStaticRGB(155, 29, 194, 75)
    }

    @Expose
    @ConfigOption(name = "Title", desc = "")
    @Accordion
    val title: TitleConfig = TitleConfig()

    class TitleConfig {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Show a title when coming into line of sight with a dragon egg.")
        @ConfigEditorBoolean
        @FeatureToggle
        var enabled: Boolean = false

        @Expose
        @ConfigOption(name = "Title Text", desc = "What the text of the title should be.")
        @ConfigEditorText
        var text: String = "§5§lDark Monolith"

    }

    @Expose
    @ConfigOption(
        name = "Tracker Settings",
        desc = "",
    )
    @Accordion
    val perTrackerConfig: IndividualItemTrackerConfig = IndividualItemTrackerConfig()

    companion object {
        @Suppress("StorageVarOrVal")
        @Transient
        internal const val DEFAULT_TITLE: String = "§5§lDark Monolith"
    }
}
