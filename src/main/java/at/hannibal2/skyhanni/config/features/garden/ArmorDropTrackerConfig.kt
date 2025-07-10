package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ArmorDropTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Track all §9Cropie§7, §5Squash §7and §6Fermento §7dropped.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Chat", desc = "Hide the chat message when receiving a farming armor drop.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideChat: Boolean = false

    @Expose
    @ConfigLink(owner = ArmorDropTrackerConfig::class, field = "enabled")
    val position: Position = Position(16, -232)
}
