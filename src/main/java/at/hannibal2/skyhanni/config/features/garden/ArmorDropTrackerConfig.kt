package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.TopLevelTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.generic.GardenTrackerSettings
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.PerTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ArmorDropTrackerConfig : TopLevelTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Track all §aCropie§7, §9Squash§7, §5Fermento §7and §6Helianthus §7dropped.")
    @ConfigEditorBoolean
    @FeatureToggle
    override var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Chat", desc = "Hide the chat message when receiving a farming armor drop.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideChat: Boolean = false

    @Expose
    @ConfigOption(name = "Tracker Settings", desc = "")
    @Accordion
    override val perTrackerConfig: PerTrackerConfig<GardenTrackerSettings> = PerTrackerConfig()

    @Expose
    @ConfigLink(owner = ArmorDropTrackerConfig::class, field = "enabled")
    override val position: Position = Position(16, -232)
}
