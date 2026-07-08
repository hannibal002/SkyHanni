package at.hannibal2.skyhanni.config.features.inventory.experimentationtable

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentsDryStreakDisplay
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ExperimentsDryStreakConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Display attempts and or XP since your last ULTRA-RARE.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Attempts", desc = "Display Attempts since.")
    @ConfigEditorBoolean
    var attemptsSince: Boolean = true

    @Expose
    @ConfigOption(name = "XP", desc = "Display XP since.")
    @ConfigEditorBoolean
    var xpSince: Boolean = true

    @ConfigOption(name = "Reset Dry Streak", desc = "Manually resets the dry-streak counter to zero. This cannot be undone.")
    @ConfigEditorButton(buttonText = "Reset")
    val resetDryStreak: Runnable = Runnable { ExperimentsDryStreakDisplay.resetManually() }

    @Expose
    @ConfigLink(owner = ExperimentsDryStreakConfig::class, field = "enabled")
    val position: Position = Position(200, -187)
}
