package at.hannibal2.skyhanni.config.features.fishing

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class FishingBaitDisplayConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Show the current fishing bait while holding a fishing rod.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigLink(owner = FishingBaitDisplayConfig::class, field = "enabled")
    val position: Position = Position(260, -15)

    @Expose
    @ConfigOption(name = "Show bait icon", desc = "Display an icon next to the Fishing Bait Display.")
    @ConfigEditorBoolean
    val showIcon: Property<Boolean> = Property.of(true)
}
