package at.hannibal2.skyhanni.config.features.event.yearofthepig

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

class ShinyOrbTrackerConfig : TopLevelTrackerConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show a tracker for Shiny Orb rewards.")
    @FeatureToggle
    @ConfigEditorBoolean
    override var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Only Holding Items", desc = "Only show the tracker while holding a Shiny Orb or Shiny Rod.")
    @ConfigEditorBoolean
    var holdingItems: Boolean = false

    @Expose
    @ConfigOption(name = "Tracker Settings", desc = "")
    @Accordion
    override val perTrackerConfig: PerTrackerConfig<ItemTrackerSettings> = PerTrackerConfig()

    @Expose
    @ConfigLink(owner = ShinyOrbTrackerConfig::class, field = "enabled")
    override val position: Position = Position(100, 100)
}
