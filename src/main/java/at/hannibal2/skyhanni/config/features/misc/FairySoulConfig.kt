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
    var fastFairySouls: Boolean = true

    @Expose
    @ConfigOption(
        name = "Fairy Soul Overlay",
        desc = "Enables the Fairy Soul overlay in the quest menu."
    )
    @FeatureToggle
    @ConfigEditorBoolean
    var fairySoulOverlay: Boolean = true

    @Expose
    @ConfigOption(
        name = "Fairy Soul Stack Size",
        desc = "Enables the display of remaining Fairy Souls in the stack size."
    )
    @FeatureToggle
    @ConfigEditorBoolean
    var fairySoulStackSize: Boolean = true

    @Expose
    @ConfigLink(owner = FairySoulConfig::class, field = "fairySoulQuestOverlay")
    val pos: Position = Position(445, 225)
}
