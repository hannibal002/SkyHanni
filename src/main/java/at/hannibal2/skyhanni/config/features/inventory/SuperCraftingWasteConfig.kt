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
        desc = "Warns you when you can save more than Xm coins by instant buying the item and instant selling the materials.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Minimum Amount", desc = "The minimum amount of coins (in millions) you must save.")
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 50.0f, minStep = 0.1f)
    var normal: Double = 10.0

    @Expose
    @ConfigOption(
        name = "Minimum Amount if Max Resource Usage",
        desc = "Minimum amount of coins when compacting items due to space reasons.",
    )
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 50.0f, minStep = 0.1f)
    var maxResource: Double = 5.0

    @Expose
    @Accordion
    @ConfigOption(name = "Values without Cookie", desc = "Like the others but when you don't have Cookie Buff active → no /bz access")
    var withoutCookieValues = SuperCraftingWasteConfigWithoutCookieConfig()
}
