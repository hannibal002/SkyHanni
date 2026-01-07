package at.hannibal2.skyhanni.config.features.inventory

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SuperCraftingWithoutCookieConfig {

    @Expose
    @ConfigOption(
        name = "Threshold",
        desc = "Minimum savings (in millions) to trigger warning without Cookie Buff.",
    )
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 100.0f, minStep = 0.1f)
    var threshold: Double = 20.0

    @Expose
    @ConfigOption(
        name = "Bulk Threshold",
        desc = "Minimum savings (in millions) when crafting maximum amount without Cookie Buff.",
    )
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 100.0f, minStep = 0.1f)
    var bulkThreshold: Double = 10.0
}
