package at.hannibal2.skyhanni.config.features.garden.pests

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class PestProfitTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Count all items you pick up when killing pests.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Hide messages", desc = "Hide regular pest drop messages.")
    @ConfigEditorBoolean
    var hideChat: Boolean = true

    @Expose
    @ConfigOption(name = "Time Displayed", desc = "Time displayed after killing a pest.")
    @ConfigEditorSlider(minValue = 5f, maxValue = 60f, minStep = 1f)
    var timeDisplayed: Int = 30

    @Expose
    @ConfigOption(name = "Include Bits", desc = "Add bits gained from killing pests to the tracker.")
    @ConfigEditorBoolean
    val includeBits: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Coins Per Bit", desc = "Set how much bits gained from killing pests are worth.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 2000f, minStep = 100f)
    val coinsPerBit: Property<Int> = Property.of(700)

    @Expose
    @ConfigLink(owner = PestProfitTrackerConfig::class, field = "enabled")
    val position: Position = Position(20, 20)
}
