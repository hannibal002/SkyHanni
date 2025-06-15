package at.hannibal2.skyhanni.config.features.garden.contest

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ContestTimesConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show the time and missing FF for every crop inside Jacob's Farming Contest inventory."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Custom BPS", desc = "")
    @Accordion
    val customBPS: CustomBPSConfig = CustomBPSConfig()

    @Expose
    @ConfigLink(owner = ContestTimesConfig::class, field = "enabled")
    val position: Position = Position(-359, 149)

    class CustomBPSConfig {
        @Expose
        @ConfigOption(
            name = "Custom BPS",
            desc = "Use custom Blocks per Second value in some GUIs instead of the real one."
        )
        @ConfigEditorBoolean
        var enabled: Boolean = true

        // TODO Write ConditionalUtils.onToggle()-s for these values in their feature classes
        @Expose
        @ConfigOption(name = "Custom BPS Value", desc = "Set a custom Blocks per Second value.")
        @ConfigEditorSlider(minValue = 15f, maxValue = 20f, minStep = 0.1f)
        var value: Double = 19.9
    }
}
