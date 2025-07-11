package at.hannibal2.skyhanni.config.features.dungeon

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class TrinityConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Sends a title when Trinity is likely to appear on dungeon (when there are 5 puzzles).",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Notify party", desc = "Automatically send message to party to watch out for Trinity.")
    @ConfigEditorBoolean
    var sendPartyChat: Boolean = false

}
