package at.hannibal2.skyhanni.config.features.pets.display.visual

import at.hannibal2.skyhanni.config.storage.Resettable
import at.hannibal2.skyhanni.utils.renderables.animated.OrbitDirection
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class ExpSharePetOrganizationConfig : Resettable {
    @Expose
    @ConfigOption(
        name = "Placement Location",
        desc = "Where the other pets should be displayed, relative to the main pet icon."
    )
    @ConfigEditorDropdown
    val placement: Property<ExpShareLocationOption> = Property.of(ExpShareLocationOption.RIGHT)

    enum class ExpShareLocationOption(private val displayName: String) {
        TOP("Top"),
        BOTTOM("Bottom"),
        LEFT("Left"),
        RIGHT("Right"),
        ORBIT("Orbit"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Group Orientation",
        desc = "How the group icons should be oriented.\n" +
            "Does not apply to Orbit mode."
    )
    @ConfigEditorDropdown
    val groupOrientation: Property<GroupOrientation> = Property.of(GroupOrientation.VERTICAL)

    enum class GroupOrientation(private val displayName: String) {
        HORIZONTAL("Horizontally"),
        VERTICAL("Vertically"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Orbit Customization", desc = "")
    @Accordion
    val subOrbit: SubOrbitConfig = SubOrbitConfig()

    class SubOrbitConfig {
        @Expose
        @ConfigOption(name = "Orbit distance", desc = "How far the icons should be separated from the main icon.")
        @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
        val orbitDistance: Property<Float> = Property.of(1f)

        @Expose
        @ConfigOption(name = "Orbit Direction", desc = "Which direction the icons should rotate.")
        @ConfigEditorDropdown
        val orbitDirection: Property<OrbitDirection> = Property.of(OrbitDirection.CLOCKWISE)

        @Expose
        @ConfigOption(name = "Orbit Speed", desc = "How fast in degrees per second the icons should rotate.")
        @ConfigEditorSlider(minValue = 10f, maxValue = 360f, minStep = 10f)
        val orbitSpeed: Property<Float> = Property.of(20f)
    }

    @ConfigOption(name = "Reset Organization", desc = "Reset the organization settings to the default values.")
    @ConfigEditorButton(buttonText = "Reset")
    val reset: Runnable = Runnable(::reset)
}
