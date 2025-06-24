package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.OnlyModern
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class TreeGiftTrackerConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Track drops from tree gifts.")
    @ConfigEditorBoolean
    @OnlyModern
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Show Total Trees", desc = "Estimate how many total trees you have chopped down.")
    @ConfigEditorBoolean
    @OnlyModern
    var showTotalTrees: Boolean = true

}
