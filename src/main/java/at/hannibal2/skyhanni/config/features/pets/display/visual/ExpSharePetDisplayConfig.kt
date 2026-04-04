package at.hannibal2.skyhanni.config.features.pets.display.visual

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.ConfigOrder
import io.github.notenoughupdates.moulconfig.annotations.ConfigOverride
import io.github.notenoughupdates.moulconfig.observer.Property

class ExpSharePetDisplayConfig(
    @field:ConfigOverride
    override val scalar: Float = 0.6f,
) : VisualPetDisplayConfig(scalar) {

    // Field @ConfigOrder values intentionally override parent orders from VisualPetDisplayConfig
    // to produce the display sequence: enabled(10), organization(20), icon(30), petItem(40),
    // rarityBackground(50), activeSlotsOnly(60). Do not remove or change without updating all six.

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
        val iconSpacing: Property<Int> = Property.of(1)
    }

    @Expose
    @ConfigOption(name = "Pet Item", desc = "")
    @Accordion
    @ConfigOverride(overrideOrder = 40)
    @ConfigOrder(40)
    override val petItem: PetItemConfig = PetItemConfig(scalar)

    @Expose
    @ConfigOption(name = "Background Color", desc = "")
    @Accordion
    @ConfigOverride(overrideOrder = 50)
    @ConfigOrder(50)
    override val rarityBackground: ExpShareBackgroundColorConfig = ExpShareBackgroundColorConfig(scalar)

    open class ExpShareBackgroundColorConfig(
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

            @ConfigOption(
                name = "Static Borders",
                desc = "Due to limitations in information, exp share pet XP rings will always be shown as full.\n" +
                    "This may change in the future."
            )
            @ConfigEditorInfoText
            @ConfigOrder(35)
            val staticNote: Unit = Unit
        }

        @Expose
        @ConfigOption(name = "XP Ring", desc = "")
        @Accordion
        @ConfigOverride
        override val borderRing = ExpShareBorderRingConfig(scalar)
    }

    @Expose
    @ConfigOption(
        name = "Active Slots Only",
        desc = "Only show pets from Exp Share slots if that slot is unlocked by Diana's §dSharing is Caring §7Perk."
    )
    @ConfigEditorBoolean
    @ConfigOrder(60)
    val activeSlotsOnly: Property<Boolean> = Property.of(true)
}
