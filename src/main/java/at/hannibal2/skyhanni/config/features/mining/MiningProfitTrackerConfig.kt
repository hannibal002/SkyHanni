package at.hannibal2.skyhanni.config.features.mining

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import at.hannibal2.skyhanni.config.features.misc.tracker.IndividualItemTrackerConfig
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MiningProfitTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable the mining profit tracker. Use §e/shresetminingtracker §7to manually reset it.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Gemstone Calculation Type", desc = "Use a different tier gemstone price in case of highly manipulated bazaar prices")
    @ConfigEditorDropdown
    var gemstoneType: GemstoneType = GemstoneType.FLAWLESS

    enum class GemstoneType(val displayName: String) {
        ROUGH("Rough"),
        FLAWED("Flawed"),
        FINE("Fine"),
        FLAWLESS("Flawless"),
        DEFAULT("Default")
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Tracker Time settings",
        desc = "Choose how the tracker behaves when you stop mining.\n" +
            "§ePause: Pauses the timer after timeout.\n" +
            "§eReset: Resets the data after timeout.\n" +
            "§eAlways On: Timer keeps running even if you stop mining."
    )
    @Accordion
    val perTrackerConfig: IndividualItemTrackerConfig = IndividualItemTrackerConfig()

    @Expose
    @ConfigLink(owner = MiningProfitTrackerConfig::class, field = "enabled")
    val position: Position = Position(100, 50)
}
