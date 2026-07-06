package at.hannibal2.skyhanni.config.features.fishing

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CommonSeaCreatureHiderConfig {
    @Expose
    @ConfigOption(
        name = "Enable Sea Creature Filter",
        desc = "Reduce visibility of common sea creatures."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Only While Holding Rod",
        desc = "Only apply the filter while holding a fishing rod."
    )
    @ConfigEditorBoolean
    var onlyWhileHoldingRod: Boolean = true

    @Expose
    @ConfigOption(
        name = "Visibility",
        desc = "How visible common sea creatures should remain."
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
    var transparency: Int = 50
}
