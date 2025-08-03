package at.hannibal2.skyhanni.config.features.mining

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GemstoneMoneyPerHourConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable gemstone money per hour display. Use §e/shresetgemstone §7to manually reset it.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Force NPC", desc = "Force the NPC price of gemstones to be used.")
    @ConfigEditorBoolean
    var forceNPC: Boolean = false

    @Expose
    @ConfigOption(name = "Gemstone Type", desc = "Which type of gemstone to use for the money per hour calculation.")
    @ConfigEditorDropdown
    var gemstoneType: GemstoneType = GemstoneType.FLAWLESS

    enum class GemstoneType(val displayName: String) {
        ROUGH("Rough"),
        FLAWED("Flawed"),
        FINE("Fine"),
        FLAWLESS("Flawless"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Timeout Time", desc = "How long the display should wait (in seconds) after your last pristine message to reset.")
    @ConfigEditorSlider(minValue = 10f, maxValue = 30f, minStep = 1f)
    var timeoutTime: Float = 15f

    @Expose
    @ConfigOption(
        name = "Pause Time",
        desc = "Whether the timer should pause instead of resetting.\n" +
            "§eNote: It will still reset when you enter a non-mining island."
    )
    @ConfigEditorBoolean
    var shouldPause: Boolean = true

    @Expose
    @ConfigLink(owner = GemstoneMoneyPerHourConfig::class, field = "enabled")
    val position: Position = Position(189, 52)
}
