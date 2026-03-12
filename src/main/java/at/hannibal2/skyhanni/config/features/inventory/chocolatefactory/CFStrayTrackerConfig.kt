package at.hannibal2.skyhanni.config.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.TopLevelTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.generic.TrackerGenericConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.IndividualTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CFStrayTrackerConfig : TopLevelTrackerConfig<TrackerGenericConfig> {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Track stray rabbits found in the Chocolate Factory menu.")
    @ConfigEditorBoolean
    @FeatureToggle
    override var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Tracker Settings", desc = "")
    @Accordion
    override val perTrackerConfig: IndividualTrackerConfig = IndividualTrackerConfig()

    @Expose
    @ConfigLink(owner = CFStrayTrackerConfig::class, field = "enabled")
    override val position: Position = Position(300, 300)
}
