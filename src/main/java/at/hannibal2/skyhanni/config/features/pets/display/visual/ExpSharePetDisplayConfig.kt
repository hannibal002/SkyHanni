package at.hannibal2.skyhanni.config.features.pets.display.visual

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class ExpSharePetDisplayConfig : VisualPetDisplayConfig() {

    @Expose
    @ConfigOption(name = "Border Ring", desc = "")
    @Accordion
    // extend the class with additional note prop
    override val borderRing: BorderRingConfig = ExpSharePetDisplayBorderRingConfig()

    class ExpSharePetDisplayBorderRingConfig : BorderRingConfig() {
        @Expose
        @ConfigOption(
            name = "Border Ring Note",
            desc = "The border ring will not be displayed for the additional pets, only the main pet."
        )
        @ConfigEditorInfoText
        val note: Property<String> = Property.of("")
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
    @ConfigOption(
        name = "Icon Spacing",
        desc = "Spacing between icons.\n" +
            "Does not apply to Orbit mode."
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 5f, minStep = 1f)
    val iconSpacing: Property<Int> = Property.of(1)

    @Expose
    @ConfigOption(name = "Icon Scale", desc = "How large the icon should be - Default is 0.5")
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 2.0f, minStep = 0.1f)
    val iconScale: Property<Double> = Property.of(0.5)

    @Expose
    @ConfigOption(
        name = "Skin Animation",
        desc = "If your pet has an animated skin, display the animated skin for the icon."
    )
    @ConfigEditorBoolean
    val skinAnimation: Property<Boolean> = Property.of(true)
}
