package at.hannibal2.skyhanni.config.features.mining.glacite

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MineshaftTimerConfig {
    @Expose
    @ConfigOption(
        name = "Cave-in Timer",
        desc = "Shows a HUD timer counting down until the mineshaft entrance caves in."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Caution Threshold",
        desc = "Seconds remaining at which the timer turns §eyellow§7."
    )
    @ConfigEditorSlider(minStep = 1f, minValue = 1f, maxValue = 59f)
    var cautionThreshold: Int = 30

    @Expose
    @ConfigOption(
        name = "Warning Threshold",
        desc = "Seconds remaining at which the timer turns §cred§7."
    )
    @ConfigEditorSlider(minStep = 1f, minValue = 1f, maxValue = 59f)
    var warningThreshold: Int = 10

    @Expose
    @ConfigOption(
        name = "Show Time in Mineshaft",
        desc = "Also displays how long you have been in the mineshaft."
    )
    @ConfigEditorBoolean
    var showTimeInMineshaft: Boolean = true

    @Expose
    @ConfigOption(
        name = "Show Estimated Time Left",
        desc = "Estimates how long you can stay before cold reaches 100, based on your current cold rate."
    )
    @ConfigEditorBoolean
    var showEstimatedTimeLeft: Boolean = true

    @Expose
    @ConfigLink(owner = MineshaftTimerConfig::class, field = "enabled")
    var position: Position = Position(10, 10)
}
