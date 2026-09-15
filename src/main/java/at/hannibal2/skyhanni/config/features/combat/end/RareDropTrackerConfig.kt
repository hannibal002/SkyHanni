package at.hannibal2.skyhanni.config.features.combat.end

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class RareDropTrackerConfig {
    @Expose
    @ConfigOption(
        name = "Rare Drop Tracker",
        desc = "Counts the Ender Dragon pets and Tier Boost Cores you have found."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigLink(owner = RareDropTrackerConfig::class, field = "enabled")
    val position: Position = Position(320, 40)

    @Expose
    @ConfigOption(name = "Show Dragons", desc = "List the Ender Dragon pets and their dry streak.")
    @ConfigEditorBoolean
    var showDragon: Boolean = true

    @Expose
    @ConfigOption(name = "Show Golems", desc = "List the Tier Boost Cores and their dry streak.")
    @ConfigEditorBoolean
    var showGolem: Boolean = true

    @Expose
    @ConfigOption(
        name = "Show Dry Streak",
        desc = "Also show how many eligible fights you have gone without one of those drops."
    )
    @ConfigEditorBoolean
    var showDryStreak: Boolean = true
}
