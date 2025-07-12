package at.hannibal2.skyhanni.config.features.event.bingo

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CompactChatConfig {
    @Expose
    @ConfigOption(
        name = "Enable",
        desc = "Shorten chat messages about skill level ups, collection gains, " +
            "new area discoveries and SkyBlock level up messages while on Bingo."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Hide Border",
        desc = "Hide the border messages before and after the compact level up messages."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideBorder: Boolean = true

    @Expose
    @ConfigOption(
        name = "Outside Bingo",
        desc = "Compact the level up chat messages outside of a Bingo profile as well."
    )
    @ConfigEditorBoolean
    var outsideBingo: Boolean = false
}
