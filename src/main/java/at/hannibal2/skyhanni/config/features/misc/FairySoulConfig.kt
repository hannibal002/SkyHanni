package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption


class FairySoulConfig {
    @Expose
    @ConfigOption(
        name = "Fast Fairy Soul Tracking",
        desc = "Enables Fast Fairy Soul tracking."
    )
    @FeatureToggle
    @ConfigEditorBoolean
    var fastFairySouls: Boolean = false

    @Expose
    @ConfigOption(
        name = "Fairy Soul Overlay",
        desc = "Enables the Fairy Soul overlay in the Fairy Soul quest menu."
    )
    @FeatureToggle
    @ConfigEditorBoolean
    var fairySoulOverlay: Boolean = true

    @Expose
    @ConfigOption(
        name = "Fairy Soul Stack Size",
        desc = "Enables the display of remaining Fairy Souls in each island's stack size in the Fairy Soul quest menu."
    )
    @FeatureToggle
    @ConfigEditorBoolean
    var fairySoulStackSize: Boolean = true

    @Expose
    @ConfigOption(
        name = "Highlight Incomplete Islands",
        desc = "Highlights islands with missing Fairy Souls in the Fairy Soul quest menu."
    )
    @FeatureToggle
    @ConfigEditorBoolean
    var fairySoulQuestHighlight: Boolean = true

    @Expose
    @ConfigLink(owner = FairySoulConfig::class, field = "fairySoulOverlay")
    val pos: Position = Position(445, 225)
}
