package at.hannibal2.skyhanni.config.features.fishing

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FishingBobberTimerConfig {
    @Expose
    @ConfigOption(
        name = "Fishing Bobber Timer",
        desc = "Show a timer for how long the fishing bobber has been deployed",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Start on Liquid Touch",
        desc = "Start the timer when the bobber touches the water/lava, instead of when it is cast.",
    )
    @ConfigEditorBoolean
    var startOnLiquidTouch: Boolean = true

    @Expose
    @ConfigLink(owner = FishingBobberTimerConfig::class, field = "enabled")
    val pos: Position = Position(10, 10)
}
