package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SuperCraftingWasteConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Warn and block from super crafting when buying the result and selling materials saves coins.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Threshold", desc = "Minimum savings (in millions) to trigger warning.")
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 50.0f, minStep = 0.1f)
    var threshold: Double = 10.0

    @Expose
    @ConfigOption(
        name = "Bulk Threshold",
        desc = "Minimum savings (in millions) when crafting maximum amount.",
    )
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 50.0f, minStep = 0.1f)
    var bulkThreshold: Double = 5.0

    @Expose
    @Accordion
    @ConfigOption(name = "Without Cookie", desc = "")
    var withoutCookie = SuperCraftingWithoutCookieConfig()
}
