package at.hannibal2.skyhanni.config.features.pets.display.visual

import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.renderables.OrbitDirection
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class VisualPetDisplayConfig {
    @Expose
    @ConfigOption(
        name = "Pet Icon",
        desc = "Show an icon of your current pet.\n" +
            "§cRequired for any options below to work§7."
    )
    @ConfigEditorBoolean
    val icon: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Background Color",
        desc = "Display a background color of the rarity of your pet."
    )
    @ConfigEditorBoolean
    val rarityBackground: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Rarity Background Customization", desc = "")
    @Accordion
    val rarityBackgroundCustomization: RarityBackgroundConfig = RarityBackgroundConfig()

    @Expose
    @ConfigOption(
        name = "Pet Item",
        desc = "Display the pet's held item as an itemstack."
    )
    @ConfigEditorBoolean
    val petItem: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Pet Item Customization", desc = "")
    @Accordion
    val petItemCustomization: PetItemConfig = PetItemConfig()

    class PetItemConfig {
        @Expose
        @ConfigOption(
            name = "Placement",
            desc = "Where the item should be placed, relative to the pet icon."
        )
        @ConfigEditorDropdown
        val placement: Property<PetItemPlacement> = Property.of(PetItemPlacement.BOTTOM_RIGHT)

        enum class PetItemPlacement(
            val vertical: RenderUtils.VerticalAlignment,
            val horizontal: RenderUtils.HorizontalAlignment,
        ) {
            TOP_LEFT(RenderUtils.VerticalAlignment.TOP, RenderUtils.HorizontalAlignment.LEFT),
            TOP_RIGHT(RenderUtils.VerticalAlignment.TOP, RenderUtils.HorizontalAlignment.RIGHT),
            BOTTOM_LEFT(RenderUtils.VerticalAlignment.BOTTOM, RenderUtils.HorizontalAlignment.LEFT),
            BOTTOM_RIGHT(RenderUtils.VerticalAlignment.BOTTOM, RenderUtils.HorizontalAlignment.RIGHT),
            ;

            override fun toString() = "$vertical $horizontal"
        }

        @Expose
        @ConfigOption(
            name = "Item Scale",
            desc = "How large the pet item icon should be."
        )
        @ConfigEditorSlider(minValue = 0.1f, maxValue = 2.0f, minStep = 0.1f)
        val scale: Property<Float> = Property.of(1.0f)
    }

    @Expose
    @ConfigOption(
        name = "XP Ring",
        desc = "Display a ring around the background color, showing your pet's progress to next level."
    )
    @ConfigEditorBoolean
    val xpRing: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "XP Ring Customization", desc = "")
    @Accordion
    val xpRingCustomization: XpRingConfig = XpRingConfig()

    class XpRingConfig {
        @Expose
        @ConfigOption(
            name = "Ring Padding",
            desc = "How much thicker the XP Ring should be compared to the smaller circles."
        )
        @ConfigEditorSlider(minValue = 1f, maxValue = 6f, minStep = 0.5f)
        val padding: Property<Int> = Property.of(3)

        @Expose
        @ConfigOption(
            name = "Filled Ring Color",
            desc = "The color of the filled portion of the ring.\n" +
                "§7Default: §#§0§0§f§f§f§f§/#00FFFF"
        )
        @ConfigEditorColour
        val filledColor: Property<ChromaColour> = Property.of(ChromaColour.fromRGB(0, 255, 255, 0, 255))

        @Expose
        @ConfigOption(
            name = "Unfilled Ring Color",
            desc = "The color of the unfilled portion of the ring.\n" +
                "§7Default: §#§c§0§c§0§c§0§/#C0C0C0"
        )
        @ConfigEditorColour
        val unfilledColor: Property<ChromaColour> = Property.of(ChromaColour.fromRGB(192, 192, 192, 0, 255))
    }

    @Expose
    @ConfigOption(
        name = "Separator Ring",
        desc = "Adds a ring between the background and the XP ring."
    )
    @ConfigEditorBoolean
    val separatorRing: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Separator Ring Customization", desc = "")
    @Accordion
    val separatorRingCustomization: RingConfig = RingConfig()

    class RingConfig {
        @Expose
        @ConfigOption(
            name = "Ring Padding",
            desc = "How wide the ring should be."
        )
        @ConfigEditorSlider(minValue = 2f, maxValue = 10f, minStep = 0.5f)
        val padding: Property<Int> = Property.of(6)

        @Expose
        @ConfigOption(
            name = "Ring Color",
            desc = "The color of the ring.\n" +
                "§7Default: §#§8§0§8§0§8§0§/#808080"
        )
        @ConfigEditorColour
        val color: Property<ChromaColour> = Property.of(ChromaColour.fromRGB(128, 128, 128, 0, 255))
    }

    @Expose
    @ConfigOption(
        name = "Exp-Share Pets",
        desc = "Adds additional pet icons for your pets active in Exp Share.\n" +
            "§cOpen the exp share menu if information is out of date."
    )
    @ConfigEditorBoolean
    val expSharePet: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Exp-Share Pets Customization", desc = "")
    @Accordion
    val expSharePetCustomization: ExpSharePetConfig = ExpSharePetConfig()

    @Expose
    @ConfigOption(name = "Icon Spin", desc = "")
    @Accordion
    val iconSpin: IconSpinConfig = IconSpinConfig()

    class IconSpinConfig {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Spin the pet icon in place.")
        @ConfigEditorDropdown
        val direction: Property<OrbitDirection> = Property.of(OrbitDirection.NONE)

        @Expose
        @ConfigOption(name = "Spin Speed", desc = "How long in seconds it should take for one spin to complete.")
        @ConfigEditorSlider(minValue = 0.5f, maxValue = 10f, minStep = 0.5f)
        val frequency: Property<Float> = Property.of(2.0f)
    }

    @Expose
    @ConfigOption(name = "Icon Scale", desc = "How large the icon should be - Default is 1.7")
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 2.5f, minStep = 0.1f)
    val iconScale: Property<Double> = Property.of(1.7)

    @Expose
    @ConfigOption(name = "Skin Animation", desc = "If your pet has an animated skin, display the animated skin for the icon.")
    @ConfigEditorBoolean
    val skinAnimation: Property<Boolean> = Property.of(true)
}
