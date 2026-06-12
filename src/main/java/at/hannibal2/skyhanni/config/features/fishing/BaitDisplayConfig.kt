package at.hannibal2.skyhanni.config.features.fishing

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class BaitDisplayConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Show current fishing bait.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigLink(owner = BaitDisplayConfig::class, field = "enabled")
    var position: Position = Position(10, 10)

    @Expose
    @ConfigOption(name = "Show bait icon", desc = "Display an icon next to the bait name.")
    @ConfigEditorBoolean
    var showIcon: Boolean = true
}
