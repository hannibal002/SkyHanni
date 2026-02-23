package at.hannibal2.skyhanni.config.features.pets.display.visual

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class ExpSharePetDisplayConfig(
    scalar: Float = 0.6f,
) : VisualPetDisplayConfig(scalar) {
    @Suppress("CanBePrimaryConstructorProperty")
    @Transient
    override val scalar: Float = scalar

    @Expose
    @ConfigOption(
        name = "Exp-Share Pets",
        desc = "Adds additional pet icons for your pets active in Exp Share.\n" +
            "§cOpen the exp share menu if information is out of date."
    )
    @ConfigEditorBoolean
    val enabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Organization", desc = "")
    @Accordion
    val organization: ExpSharePetOrganizationConfig = ExpSharePetOrganizationConfig()

    @Expose
    @ConfigOption(name = "Pet Icon", desc = "")
    @Accordion
    override val icon: SpaceIconConfig = SpaceIconConfig(scalar)

    class SpaceIconConfig(scalar: Float = 1.0f) : IconConfig(scalar) {
        companion object {
            private const val DEFAULT_ICON_SPACING = 1
        }

        @Expose
        @ConfigOption(
            name = "Icon Spacing",
            desc = "Spacing between icons.\n" +
                "Does not apply to Orbit mode."
        )
        @ConfigEditorSlider(minValue = 1f, maxValue = 5f, minStep = 1f)
        val iconSpacing: Property<Int> = Property.of(DEFAULT_ICON_SPACING)
    }

    @Expose
    @ConfigOption(name = "Pet Item", desc = "")
    @Accordion
    override val petItem: PetItemConfig = PetItemConfig(scalar)

    @Expose
    @ConfigOption(name = "Background Color", desc = "")
    @Accordion
    override val rarityBackground: ExpShareBackgroundColorConfig = ExpShareBackgroundColorConfig(scalar)

    open class ExpShareBackgroundColorConfig(
        scalar: Float = 1.0f,
    ) : BackgroundColorConfig(scalar) {
        @Expose
        @ConfigOption(name = "XP Ring", desc = "")
        @Accordion
        override val borderRing = object : BorderRingConfig(scalar) {
            @Expose
            @ConfigOption(
                name = "Enabled",
                desc = "Display a border ring around the background color.",
            )
            @ConfigEditorBoolean
            override val enabled: Property<Boolean> = Property.of(true)

            @Expose
            @ConfigOption(name = "Separator Ring", desc = "")
            @Accordion
            override val separator: SeparatorRingConfig = SeparatorRingConfig(scalar)

            @ConfigOption(
                name = "Static Borders",
                desc = "Due to limitations in information, exp share pet XP rings will always be shown as full.\n" +
                    "This may change in the future."
            )
            @ConfigEditorInfoText
            val staticNote: Unit = Unit
        }
    }

    @Expose
    @ConfigOption(
        name = "Active Slots Only",
        desc = "Only show pets from Exp Share slots if that slot is unlocked by Diana's §dSharing is Caring §7Perk."
    )
    @ConfigEditorBoolean
    val activeSlotsOnly: Property<Boolean> = Property.of(true)
}
