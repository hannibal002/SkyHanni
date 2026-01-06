package at.hannibal2.skyhanni.config.features.inventory

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SuperCraftingWasteConfigWithoutCookie {

    @Expose
    @ConfigOption(
        name = "Minimum Amount",
        desc = "The minimum amount of coins (in millions) you must save (instant sell and insta buy " +
            "for wanted) to get a warning.",
    )
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 100.0f, minStep = 0.1f)
    val normal: Double = 20.0

    @Expose
    @ConfigOption(
        name = "Minimum Amount if Max Resource Usage",
        desc = "Minimum amount of coins when compacting items due to space reasons.",
    )
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 100.0f, minStep = 0.1f)
    val maxResource: Double = 10.0
}
