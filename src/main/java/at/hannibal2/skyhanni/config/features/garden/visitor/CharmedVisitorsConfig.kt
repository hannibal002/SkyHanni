package at.hannibal2.skyhanni.config.features.garden.visitor

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CharmedVisitorsConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Show a list of visitors you have charmed with the Gift Vinyl Set.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigLink(owner = CharmedVisitorsConfig::class, field = "enabled")
    val position: Position = Position(180, 200)
}
