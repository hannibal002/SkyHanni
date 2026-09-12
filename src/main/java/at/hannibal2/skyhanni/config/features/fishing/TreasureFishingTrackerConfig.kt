package at.hannibal2.skyhanni.config.features.fishing

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.IndividualTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class TreasureFishingTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Count good, great, and outstanding treasure fishing catches.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigLink(owner = TreasureFishingTrackerConfig::class, field = "enabled")
    val position: Position = Position(20, 20)

    @Expose
    @ConfigOption(name = "Show Percentage", desc = "Show percentage how often each treasure catch type got caught.")
    @ConfigEditorBoolean
    val showPercentage: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Tracker Settings",
        desc = "",
    )
    @Accordion
    val perTrackerConfig: IndividualTrackerConfig = IndividualTrackerConfig()
}
