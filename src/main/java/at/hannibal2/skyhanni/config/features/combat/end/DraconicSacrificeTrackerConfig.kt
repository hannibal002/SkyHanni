package at.hannibal2.skyhanni.config.features.combat.end

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.generic.ItemTrackerSettings
import at.hannibal2.skyhanni.config.features.misc.tracker.TopLevelTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.PerTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DraconicSacrificeTrackerConfig : TopLevelTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Tracks items and profit while using the Draconic Altar in the End.")
    @ConfigEditorBoolean
    @FeatureToggle
    override var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Only In Void Slate", desc = "Show the tracker only when inside the Void Slate area.")
    @ConfigEditorBoolean
    var onlyInVoidSlate: Boolean = true

    @Expose
    @ConfigOption(name = "Tracker Settings", desc = "")
    @Accordion
    override val perTrackerConfig: PerTrackerConfig<ItemTrackerSettings> = PerTrackerConfig()

    @Expose
    @ConfigLink(owner = DraconicSacrificeTrackerConfig::class, field = "enabled")
    override val position: Position = Position(201, 199)
}
