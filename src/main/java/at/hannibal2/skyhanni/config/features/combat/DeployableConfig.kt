package at.hannibal2.skyhanni.config.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.combat.DeployableType
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DeployableConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Show active deployables.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Only Show Highest Tier",
        desc = "Only show highest tier of deployable.\n" +
            "e.g. Only show Overflux if a Radiant and Overflux are placed.",
    )
    @ConfigEditorBoolean
    var highestTierOnly: Boolean = true

    @Expose
    @ConfigOption(name = "Deployable Types", desc = "Which types of Deployables to display.")
    @ConfigEditorDraggableList
    val displayTypes: MutableList<DeployableType> = mutableListOf(
        DeployableType.FLUX,
        DeployableType.LANTERN,
        DeployableType.UMBERELLA,
    )

    @Expose
    @ConfigOption(name = "Missing Deployable Warning", desc = "Warn when a deployable is missing.")
    @ConfigEditorBoolean
    @FeatureToggle
    var warnMissingDeployable: Boolean = false

    @Expose
    @ConfigOption(name = "Deployable Warnings", desc = "Which deployables to warn for.")
    @ConfigEditorDraggableList
    val warningTypes: MutableList<WarningType> = mutableListOf(
        WarningType.SLAYER,
        WarningType.TROPHY_FISHING
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

    @ConfigOption(
        name = "Flare Display",
        desc = "Flares have their own settings.",
    )
    @ConfigEditorButton(buttonText = "Go")
    val flareRunnable = Runnable { SkyHanniMod.feature.combat.flare::enabled.jumpToEditor() }

    @ConfigOption(
        name = "Totem of Corruption",
        desc = "Totem of Corruption has its own settings.",
    )
    @ConfigEditorButton(buttonText = "Go")
    val totemRunnable = Runnable { SkyHanniMod.feature.fishing.totemOfCorruption::showOverlay.jumpToEditor() }

    @Expose
    @ConfigLink(owner = DeployableConfig::class, field = "enabled")
    val position: Position = Position(-160, -70)

    @Expose
    @ConfigLink(owner = DeployableConfig::class, field = "warnMissingDeployable")
    val warningPosition: Position = Position(10, 10)
}
