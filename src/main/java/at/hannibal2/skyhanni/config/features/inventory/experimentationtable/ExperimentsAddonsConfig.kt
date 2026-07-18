package at.hannibal2.skyhanni.config.features.inventory.experimentationtable

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.OnlyDebug
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class ExperimentsAddonsConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Enable the helper for Chronomatron and Ultrasequencer.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Next Click Helper",
        desc = "Highlights the next slot to click in Chronomatron, and shows all items in Ultrasequencer.",
    )
    @ConfigEditorBoolean
    var highlightNextClick: Boolean = true

    @Expose
    @ConfigOption(name = "Color", desc = "Color that the next slot will be highlighted in.")
    @ConfigEditorColour
    var nextColor: ChromaColour = LorenzColor.GREEN.toChromaColor()

    @Expose
    @ConfigOption(name = "Second Color", desc = "Color that the second slot will be highlighted in.")
    @ConfigEditorColour
    var secondColor: ChromaColour = LorenzColor.YELLOW.toChromaColor(128)

    @Expose
    @ConfigOption(
        name = "Prevent Misclicks",
        desc = "Prevent clicking wrong colors in Chronomatron, and wrong slots in Ultrasequencer.",
    )
    @ConfigEditorBoolean
    @SearchTag("missclick")
    var preventMisclicks: Boolean = true

    @Expose
    @ConfigOption(
        name = "Max Clicks Alert",
        desc = "Display an alert when you reach the maximum clicks gained from Chronomatron or Ultrasequencer.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var maxSequenceAlert: Boolean = true

    @Expose
    @ConfigOption(name = "Addons Debug", desc = "Enable extra Superpairs Addons debug info.")
    @ConfigEditorBoolean
    @OnlyDebug
    var addonsDebug: Boolean = false

    @Expose
    @ConfigLink(owner = ExperimentsAddonsConfig::class, field = "addonsDebug")
    val addonsDebugPosition: Position = Position(300, 300)
}
