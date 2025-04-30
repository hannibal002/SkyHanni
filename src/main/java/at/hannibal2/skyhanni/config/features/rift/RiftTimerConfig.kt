package at.hannibal2.skyhanni.config.features.rift

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class RiftTimerConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show the remaining rift time, max time, percentage, and extra time changes."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Max Time", desc = "Show max time.")
    @ConfigEditorBoolean
    var maxTime: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Percentage", desc = "Show percentage.")
    @ConfigEditorBoolean
    var percentage: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigLink(owner = RiftTimerConfig::class, field = "enabled")
    var timerPosition: Position = Position(10, 10)

    @Expose
    @ConfigOption(name = "Nametag Format", desc = "Format the remaining rift time for other players in their nametag.")
    @ConfigEditorBoolean
    @FeatureToggle
    var nametag: Boolean = true
}
