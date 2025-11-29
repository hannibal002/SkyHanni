package at.hannibal2.skyhanni.config.features.event.waypoints

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class HalloweenBasketConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show all Halloween Basket waypoints.\n" +
            "§eCoordinates may not always be up to date!"
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val enabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Only Closest", desc = "Only show the closest waypoint.")
    @ConfigEditorBoolean
    var onlyClosest: Boolean = true

    @Expose
    @ConfigOption(name = "Pathfinder", desc = "Show a path to the closest basket.")
    @ConfigEditorBoolean
    @FeatureToggle
    val pathfind: Property<Boolean> = Property.of(true)
}
