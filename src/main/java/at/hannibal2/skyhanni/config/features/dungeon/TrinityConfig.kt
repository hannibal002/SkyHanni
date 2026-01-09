package at.hannibal2.skyhanni.config.features.dungeon

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class TrinityConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Sends a title when Trinity is likely to appear in the dungeon (when there are 5 puzzles).",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Notify Party", desc = "Automatically send a message to the party to watch out for Trinity.")
    @ConfigEditorBoolean
    var sendPartyChat: Boolean = false

}
