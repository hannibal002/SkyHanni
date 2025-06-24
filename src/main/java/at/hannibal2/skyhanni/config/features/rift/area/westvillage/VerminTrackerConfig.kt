package at.hannibal2.skyhanni.config.features.rift.area.westvillage

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class VerminTrackerConfig {
    @Expose
    @ConfigOption(name = "Show Counter", desc = "Count all §aSilverfish§7, §aSpiders, §7and §aFlies §7vacuumed.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Show Outside West Village", desc = "Show the Vermin Tracker in other areas of The Rift.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showOutsideWestVillage: Boolean = false

    @Expose
    @ConfigOption(name = "Show without Vacuum", desc = "Require having Turbomax Vacuum in your inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showWithoutVacuum: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Chat", desc = "Hide the chat message when vacuuming a vermin.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideChat: Boolean = false

    @Expose
    @ConfigLink(owner = VerminTrackerConfig::class, field = "enabled")
    var position: Position = Position(16, -232)
}

