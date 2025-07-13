package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HideFarEntitiesConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Hide all unnecessary entities from rendering except the nearest ones.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Min Distance", desc = "Always show mobs that are at least that close to the player.")
    @ConfigEditorSlider(minValue = 3f, maxValue = 30f, minStep = 1f)
    var minDistance: Int = 10

    @Expose
    @ConfigOption(name = "Max Amount", desc = "Not showing more than this amount of nearest entities.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 150f, minStep = 1f)
    var maxAmount: Int = 30
}
