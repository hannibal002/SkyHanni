package at.hannibal2.skyhanni.config.features.combat

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.combat.DeployableType
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DeployableReminderConfig {
    @Expose
    @ConfigOption(name = "Missing Deployable Warning", desc = "Warn when a deployable is missing.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Deployable Warnings",
        desc = "Warn when the required deployable is missing: Flux for Slayer, Umberella for Trophy Fishing, and Lantern for Mineshafts.",
    )
    @ConfigEditorDraggableList
    val warningTypes: MutableList<WarningType> = mutableListOf(
        WarningType.SLAYER,
        WarningType.TROPHY_FISHING,
        WarningType.MINESHAFT,
    )

    enum class WarningType(val displayName: String, val deployableType: DeployableType) {
        SLAYER("Slayer Boss Fight", DeployableType.FLUX),
        TROPHY_FISHING("Trophy Fishing", DeployableType.UMBERELLA),
        MINESHAFT("Mineshaft", DeployableType.LANTERN),
        ;

        override fun toString(): String {
            return displayName + " (${deployableType.name.allLettersFirstUppercase()})"
        }
    }

    @Expose
    @ConfigOption(name = "Warning Delay", desc = "Delay before warning to place deployable.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 10f, minStep = 1f)
    var warningDelay: Int = 5

    @Expose
    @ConfigOption(name = "Warning Duration", desc = "Duration to show warning to place deployable.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 0.5f)
    var warningDuration: Double = 3.0

    @Expose
    @ConfigLink(owner = DeployableReminderConfig::class, field = "enabled")
    val warningPosition: Position = Position(10, 10)
}
