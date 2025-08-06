package at.hannibal2.skyhanni.config.features.mining.glacite

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ExcavatorScrapGFSConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show a button while in the Fossil Excavator to Supicious Scrap from your sacks.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Fetch Amount", desc = "How many Suspicious Scrap to fetch from your sacks when clicking the button.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 64f, minStep = 1f)
    var fetchAmount: Int = 16

    @Expose
    @ConfigOption(
        name = "BZ if Sacks Empty",
        desc = "If you do not have any Suspicious Scrap in your sacks, the bazaar will be opened to buy Suspicious Scrap."
    )
    @ConfigEditorBoolean
    var bzIfSacksEmpty: Boolean = false

}
