package at.hannibal2.skyhanni.config.features.garden.composter

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class NotifyLowConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Show a notification when Organic Matter or Fuel runs low in your Composter.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Show Title", desc = "Send a title to notify.")
    @ConfigEditorBoolean
    var title: Boolean = false

    @Expose
    @ConfigOption(name = "Min Organic Matter", desc = "Warn when Organic Matter is below this value.")
    @ConfigEditorSlider(minValue = 1000f, maxValue = 80000f, minStep = 100f)
    var organicMatter: Int = 20000

    @Expose
    @ConfigOption(name = "Min Fuel", desc = "Warn when Fuel is below this value.")
    @ConfigEditorSlider(minValue = 500f, maxValue = 40000f, minStep = 100f)
    var fuel: Int = 10000
}
