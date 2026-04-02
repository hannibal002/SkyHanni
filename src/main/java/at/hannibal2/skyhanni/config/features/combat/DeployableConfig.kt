package at.hannibal2.skyhanni.config.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.combat.DeployableDisplay
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
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
    val displayTypes: MutableList<DeployableDisplay.DeployableType> = mutableListOf(
        DeployableDisplay.DeployableType.FLUX,
        DeployableDisplay.DeployableType.LANTERN,
        DeployableDisplay.DeployableType.UMBERELLA,
    )

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
}
