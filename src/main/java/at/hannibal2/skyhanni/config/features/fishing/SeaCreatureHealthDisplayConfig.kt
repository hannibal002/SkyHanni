package at.hannibal2.skyhanni.config.features.fishing

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SeaCreatureHealthDisplayConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Shows a GUI with the health of the Sea Creatures Selected.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled = false

    @Expose
    @ConfigOption(name = "Limit", desc = "The maximum amount of mobs to show.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    var limit = 5

    @ConfigOption(name = "Custom Health Display Mobs", desc = "This Feature's Mobs can be customized under /shseacreatures!")
    @ConfigEditorInfoText
    var notice: String = ""

    @Expose
    @ConfigLink(owner = SeaCreatureHealthDisplayConfig::class, field = "enabled")
    val pos = Position(200, 200, centerX = true)
}
