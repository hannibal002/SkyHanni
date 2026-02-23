package at.hannibal2.skyhanni.config.features.pets.display.visual

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class ExpSharePetDisplayConfig : VisualPetDisplayConfig() {
    @Expose
    @ConfigOption(
        name = "Exp-Share Pets",
        desc = "Adds additional pet icons for your pets active in Exp Share.\n" +
            "§cOpen the exp share menu if information is out of date."
    )
    @ConfigEditorBoolean
    val enabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Pet Icon", desc = "")
    @Accordion
    override val icon: SpaceIconConfig = SpaceIconConfig()

    class SpaceIconConfig : IconConfig() {
        @Expose
        @ConfigOption(
            name = "Icon Spacing",
            desc = "Spacing between icons.\n" +
                "Does not apply to Orbit mode."
        )
        @ConfigEditorSlider(minValue = 1f, maxValue = 5f, minStep = 1f)
        val iconSpacing: Property<Int> = Property.of(1)
    }

    @Expose
    @ConfigOption(name = "Background Color", desc = "")
    @Accordion
    override val rarityBackground: ExpShareBackgroundColorConfig = ExpShareBackgroundColorConfig()

    open class ExpShareBackgroundColorConfig : BackgroundColorConfig() {
        @Expose
        @ConfigOption(name = "XP Ring", desc = "")
        @Accordion
        override val borderRing = object : BorderRingConfig() {
            @Expose
            @ConfigOption(
                name = "Enabled",
                desc = "Display a border ring around the background color.",
            )
            @ConfigEditorBoolean
            override val enabled: Property<Boolean> = Property.of(true)

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
    @ConfigOption(name = "Organization", desc = "")
    @Accordion
    val organization: ExpSharePetOrganizationConfig = ExpSharePetOrganizationConfig()

    @Expose
    @ConfigOption(
        name = "Active Slots Only",
        desc = "Only show pets from Exp Share slots if that slot is unlocked by Diana's §dSharing is Caring §7Perk."
    )
    @ConfigEditorBoolean
    val activeSlotsOnly: Property<Boolean> = Property.of(true)
}
