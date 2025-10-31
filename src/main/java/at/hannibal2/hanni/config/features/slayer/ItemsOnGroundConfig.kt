package at.hannibal2.hanni.config.features.slayer

import at.hannibal2.hanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ItemsOnGroundConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show the name and price of items laying on the ground. §cOnly in slayer areas!"
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Minimum Price", desc = "Items below this price will be ignored.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 1000000f, minStep = 1f)
    var minimumPrice: Int = 50000
}
