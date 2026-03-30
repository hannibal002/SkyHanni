package at.hannibal2.skyhanni.config.features.combat

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.combat.DeployableDisplay
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DeployableConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Show active deployables")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Only Show Highest Tier", desc = "Only show highest tier of deployable.\n" +
            "e.g. Only show Overflux if a Radiant and Overflux are placed")
    @ConfigEditorBoolean
    @FeatureToggle
    var highestTierOnly: Boolean = true

    @Expose
    @ConfigOption(name = "Deployable Types", desc = "Which types of Deployables to display")
    @ConfigEditorDraggableList
    var displayTypes: MutableList<DeployableDisplay.DeployableType> = mutableListOf(
        DeployableDisplay.DeployableType.FLUX,
        DeployableDisplay.DeployableType.LANTERN,
        DeployableDisplay.DeployableType.UMBERELLA,
    )

    @Expose
    @ConfigLink(owner = DeployableConfig::class, field = "enabled")
    val position: Position = Position(-160, -70)
}
