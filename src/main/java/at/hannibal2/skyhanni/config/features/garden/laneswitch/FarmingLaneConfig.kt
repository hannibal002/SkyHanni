package at.hannibal2.skyhanni.config.features.garden.laneswitch

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
//#if TODO
import at.hannibal2.skyhanni.features.garden.CropType
//#endif
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

// todo 1.21 impl needed
class FarmingLaneConfig {
    @ConfigOption(
        name = "Create Lanes",
        desc = "In order for those features to work, you first need to create a lane with §e/shlanedetection§7!",
    )
    @ConfigEditorInfoText(infoTitle = "Tutorial")
    var tutorial: Boolean = false

    @Expose
    @ConfigOption(name = "Lane Switch Notification", desc = "")
    @Accordion
    var laneSwitchNotification: LaneSwitchNotificationConfig = LaneSwitchNotificationConfig()

    @Expose
    @ConfigOption(
        name = "Distance Display",
        desc = "Show the remaining distance and time until you reach the end of the current lane.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var distanceDisplay: Boolean = false

    @Expose
    @ConfigLink(owner = FarmingLaneConfig::class, field = "distanceDisplay")
    var distanceDisplayPosition: Position = Position(0, 200)

    @Expose
    @ConfigOption(name = "Corner Waypoints", desc = "Show the corner for the current lane in the world.")
    @ConfigEditorBoolean
    @FeatureToggle
    var cornerWaypoints: Boolean = false

    //#if TODO
    @Expose
    @ConfigOption(name = "Ignored Crops", desc = "Add the crops you wish to not setup a lane for.")
    @ConfigEditorDraggableList
    var ignoredCrops: MutableList<CropType> = mutableListOf()
    //#endif
}
