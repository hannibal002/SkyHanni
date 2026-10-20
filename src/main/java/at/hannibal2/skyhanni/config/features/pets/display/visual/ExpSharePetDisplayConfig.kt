package at.hannibal2.skyhanni.config.features.pets.display.visual

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.ConfigOrder
import io.github.notenoughupdates.moulconfig.annotations.ConfigOverride
import io.github.notenoughupdates.moulconfig.observer.Property

class ExpSharePetDisplayConfig(
    @field:ConfigOverride
    override val scalar: Float = 0.6f,
) : VisualPetDisplayConfig(scalar) {

    @Expose
    @ConfigOption(
        name = "Exp-Share Pets",
        desc = "Adds additional pet icons for your pets active in Exp Share.\n" +
            "§cOpen the exp share menu if information is out of date."
    )
    @ConfigEditorBoolean
    @ConfigOrder(10)
    val enabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Organization", desc = "")
    @Accordion
    @ConfigOrder(20)
    val organization: ExpSharePetOrganizationConfig = ExpSharePetOrganizationConfig()

    @Expose
    @ConfigOption(name = "Pet Icon", desc = "")
    @Accordion
    @ConfigOverride(overrideOrder = 30)
    @ConfigOrder(30)
    override val icon: SpaceIconConfig = SpaceIconConfig(scalar)

    class SpaceIconConfig(scalar: Float = 1.0f) : IconConfig(scalar) {
        @Expose
        @ConfigOption(
            name = "Icon Spacing",
            desc = "Spacing between icons.\n" +
                "Does not apply to Orbit mode."
        )
        @ConfigEditorSlider(minValue = 1f, maxValue = 5f, minStep = 1f)
        @ConfigOrder(45)
        val iconSpacing: Property<Float> = Property.of(1f)
    }

    @Expose
    @ConfigOption(name = "Background Color", desc = "")
    @Accordion
    @ConfigOverride(overrideOrder = 40)
    @ConfigOrder(40)
    override val rarityBackground: ExpShareBackgroundColorConfig = ExpShareBackgroundColorConfig(scalar)

    @Expose
    @ConfigOption(name = "Pet Item", desc = "")
    @Accordion
    @ConfigOverride(overrideOrder = 50)
    @ConfigOrder(50)
    override val petItem: PetItemConfig = PetItemConfig(scalar)

    class ExpShareBackgroundColorConfig(
        scalar: Float = 1.0f,
    ) : BackgroundColorConfig(scalar) {

        class ExpShareBorderRingConfig(scalar: Float = 1.0f) : BorderRingConfig(scalar) {
            @Expose
            @ConfigOption(
                name = "Enabled",
                desc = "Display a border ring around the background color.",
            )
            @ConfigEditorBoolean
            @ConfigOverride
            override val enabled: Property<Boolean> = Property.of(true)

            @Expose
            @ConfigOption(name = "Separator Ring", desc = "")
            @Accordion
            @ConfigOverride
            override val separator: SeparatorRingConfig = SeparatorRingConfig(scalar)

        }

        @Expose
        @ConfigOption(name = "XP Ring", desc = "")
        @Accordion
        @ConfigOverride
        override val borderRing = ExpShareBorderRingConfig(scalar)
    }

    @Expose
    @ConfigOption(
        name = "Hide Disabled Slots",
        desc = "Hide disabled Exp Share slots from the display.\n" +
            "Disabled slots are shown with reduced opacity when this is off."
    )
    @ConfigEditorBoolean
    @ConfigOrder(60)
    val activeSlotsOnly: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Disabled Opacity",
        desc = "Opacity for disabled Exp Share pets."
    )
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 1f, minStep = 0.05f)
    @ConfigOrder(65)
    val disabledOpacity: Property<Float> = Property.of(0.5f)
}
