package at.hannibal2.skyhanni.config.features.mining.glacite

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CorpseTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable the Corpse Tracker overlay for Glacite Mineshafts.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Only when in Mineshaft", desc = "Only show the overlay while in a Glacite Mineshaft.")
    @ConfigEditorBoolean
    var onlyInMineshaft: Boolean = false

    @Expose
    @ConfigLink(owner = CorpseTrackerConfig::class, field = "enabled")
    val position: Position = Position(-274, 0)
}
