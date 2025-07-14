package at.hannibal2.skyhanni.config.features.event.hoppity

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class HoppityStrayTimerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show a 30s timer in the chocolate factory after collecting a meal egg.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigLink(owner = HoppityStrayTimerConfig::class, field = "enabled")
    val strayTimerPosition: Position = Position(200, 200)

    @Expose
    @ConfigOption(
        name = "Block Closing",
        desc = "Block closing the Chocolate Factory while the timer is active.\n" +
            "§eHold §cShift §eto bypass"
    )
    @SearchTag("prevent close")
    @ConfigEditorBoolean
    var blockClosing: Boolean = false

    @Expose
    @ConfigOption(
        name = "Ding For Timer",
        desc = "Play a ding sound when the timer drops below this number. Set to 0 to disable.",
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 30f, minStep = 1f)
    var dingForTimer: Int = 3
}
