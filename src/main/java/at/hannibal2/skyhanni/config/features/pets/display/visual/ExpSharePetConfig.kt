package at.hannibal2.skyhanni.config.features.pets.display.visual

import at.hannibal2.skyhanni.utils.renderables.OrbitDirection
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class ExpSharePetConfig {
    @Expose
    @ConfigOption(
        name = "Placement",
        desc = "Where the other pets should be displayed, relative to the main pet icon."
    )
    @ConfigEditorDropdown
    val placement: Property<ExpShareLocationOption> = Property.of(ExpShareLocationOption.RIGHT)

    enum class ExpShareLocationOption(private val displayName: String) {
        TOP("Top"),
        BOTTOM("Bottom"),
        LEFT("Left"),
        RIGHT("Right"),
        ORBIT("Orbit")
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
        val orbitDistance: Property<Int> = Property.of(1)

        @Expose
        @ConfigOption(name = "Orbit Direction", desc = "Which direction the icons should rotate.")
        @ConfigEditorDropdown
        val orbitDirection: Property<OrbitDirection> = Property.of(OrbitDirection.CLOCKWISE)

        @Expose
        @ConfigOption(name = "Orbit Speed", desc = "How fast in degrees per second the icons should rotate.")
        @ConfigEditorSlider(minValue = 10f, maxValue = 360f, minStep = 10f)
        val orbitSpeed: Property<Int> = Property.of(20)
    }

    @Expose
    @ConfigOption(name = "Display Customization", desc = "")
    @Accordion
    val displayCustomization: ExpSharePetDisplayConfig = ExpSharePetDisplayConfig()

    @Expose
    @ConfigOption(
        name = "Active Slots Only",
        desc = "Only show pets from Exp Share slots if that slot is unlocked by Diana's Sharing is §dCaring §7Perk."
    )
    @ConfigEditorBoolean
    val activeSlotsOnly: Property<Boolean> = Property.of(true)
}
