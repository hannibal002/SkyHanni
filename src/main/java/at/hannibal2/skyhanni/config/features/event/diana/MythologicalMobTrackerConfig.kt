package at.hannibal2.skyhanni.config.features.event.diana

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class MythologicalMobTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Count the different mythological mobs you have dug up.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigLink(owner = MythologicalMobTrackerConfig::class, field = "enabled")
    var position: Position = Position(20, 20)

    @Expose
    @ConfigOption(name = "Show Percentage", desc = "Show percentage how often what mob spawned.")
    @ConfigEditorBoolean
    var showPercentage: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Hide Chat", desc = "Hide the chat messages when digging up a mythological mob.")
    @ConfigEditorBoolean
    var hideChat: Boolean = false
}
