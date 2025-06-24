package at.hannibal2.skyhanni.config.features.crimsonisle

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class RescueMissionConfig {
    @Expose
    @ConfigOption(
        name = "Agent Path",
        desc = "Show a path to the §eUndercover Agent §fwhen talking to the Rescue Recruter",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var agentPath: Boolean = true

    @Expose
    @ConfigOption(
        name = "Hostage Path",
        desc = "Show a path to the hostage based on your quest rank. " +
            "You must hover over the book that gives you the quest and stand near the agent for the solver to work. " +
            "§cDoes not yet work with Mage S rank!",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hostagePath: Boolean = true

    @Expose
    @ConfigOption(
        name = "Path Variant",
        desc = "For Barbarian S-tier, there are two variants. If your path seems wrong, change it to the other one.",
    )
    @ConfigEditorDropdown
    var variant: Property<PathVariant> = Property.of(PathVariant.ONE)

    @Expose
    @ConfigOption(name = "Look Ahead", desc = "Change how many waypoints should be shown in front of you.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    var lookAhead: Property<Int> = Property.of(2)

    @Expose
    @ConfigOption(name = "Rainbow Color", desc = "Show the rainbow color effect.")
    @ConfigEditorBoolean
    var chroma: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Single Color", desc = "Make the waypoints an unchanging color for slow computers.")
    @ConfigEditorColour
    var solidColor: Property<String> = Property.of("0:60:0:0:255")

    enum class PathVariant(val displayName: String) {
        ONE("1"),
        TWO("2"),
        ;

        override fun toString(): String = displayName
    }
}
