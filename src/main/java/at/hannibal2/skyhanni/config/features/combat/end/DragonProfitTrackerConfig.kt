package at.hannibal2.skyhanni.config.features.combat.end

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DragonProfitTrackerConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Keeps track of everything you pick up while fighting the dragon, " +
            "keeping track of how much you pay for starting the fight and calculating the overall profit."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigLink(owner = DragonProfitTrackerConfig::class, field = "enabled")
    var position: Position = Position(20, 20, false, true)

    @Expose
    @ConfigOption(
        name = "Count Leeched Dragons",
        desc = "Count Dragons you placed no eyes in towards your total profit."
    )
    @FeatureToggle
    @ConfigEditorBoolean
    var countLeechedDragons: Boolean = true
}
