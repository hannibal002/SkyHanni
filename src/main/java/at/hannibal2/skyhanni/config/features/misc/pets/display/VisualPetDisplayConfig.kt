package at.hannibal2.skyhanni.config.features.misc.pets.display

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
            "§cRequired for any below options to work§7."
    )
    @ConfigEditorBoolean
    val petIcon: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Background Color",
        desc = "Display a background color of the rarity of your pet.\n" +
            "§eDepends on Pet Icon being enabled above!"
    )
    @ConfigEditorBoolean
    val rarityBackground: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "XP Ring",
        desc = "Display a ring showing your pet's progress to next level.\n" +
            "§eDepends on Pet Icon & Background Color being enabled above!"
    )
    @ConfigEditorBoolean
    val xpRing: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Separator Ring",
        desc = "Adds a separator ring between the rarity background and the XP ring.\n" +
            "§eDepends on Pet Icon, Background Color, & XP Ring being enabled above!"
    )
    @ConfigEditorBoolean
    val separatorRing: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Visual Customization", desc = "")
    @Accordion
    val customization: VisualCustomizationConfig = VisualCustomizationConfig()

    class VisualCustomizationConfig {

        @Expose
        @ConfigOption(name = "Separator Ring", desc = "")
        @Accordion
        val separatorRing: SeparatorRingConfig = SeparatorRingConfig()

        class SeparatorRingConfig {
            @Expose
            @ConfigOption(
                name = "Ring Padding",
                desc = "How much thicker the Separator Ring should be compared to the smaller circles."
            )
            @ConfigEditorSlider(minValue = 2f, maxValue = 10f, minStep = 0.5f)
            val padding: Property<Int> = Property.of(6)

            @Expose
            @ConfigOption(
                name = "Ring Color",
                desc = "The color of the separator ring.\n" +
                    "§7Default: §#§8§0§8§0§8§0§/#808080"
            )
            @ConfigEditorColour
            val color: Property<ChromaColour> = Property.of(ChromaColour.fromRGB(128, 128, 128, 0, 255))
        }

        @Expose
        @ConfigOption(name = "XP Ring", desc = "")
        @Accordion
        val xpRing: XpRingConfig = XpRingConfig()

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
        @ConfigOption(name = "Rarity Background", desc = "")
        @Accordion
        val rarityBackground: RarityBackgroundConfig = RarityBackgroundConfig()

        class RarityBackgroundConfig {
            @Expose
            @ConfigOption(
                name = "Background Padding",
                desc = "How much extra padding should be added to the background circle."
            )
            @ConfigEditorSlider(minValue = 2f, maxValue = 8f, minStep = 0.5f)
            val padding: Property<Int> = Property.of(4)

            @Expose
            @ConfigOption(
                name = "§fCommon §rColor",
                desc = "§7Default: §#§f§f§f§f§f§f§/#FFFFFF"
            )
            @ConfigEditorColour
            val commonColor: Property<ChromaColour> = Property.of(ChromaColour.fromRGB(255, 255, 255, 0, 255))

            @Expose
            @ConfigOption(
                name = "§aUncommon §rColor",
                desc = "§7Default: §#§5§5§f§f§5§5§/#55FF55"
            )
            @ConfigEditorColour
            val uncommonColor: Property<ChromaColour> = Property.of(ChromaColour.fromRGB(85, 255, 85, 0, 255))

            @Expose
            @ConfigOption(
                name = "§9Rare §rColor",
                desc = "§7Default: §#§5§5§5§5§f§f§/#5555FF"
            )
            @ConfigEditorColour
            val rareColor: Property<ChromaColour> = Property.of(ChromaColour.fromRGB(85, 85, 255, 0, 255))

            @Expose
            @ConfigOption(
                name = "§5Epic §rColor",
                desc = "§7Default: §#§a§a§0§0§a§a§/#AA00AA"
            )
            @ConfigEditorColour
            val epicColor: Property<ChromaColour> = Property.of(ChromaColour.fromRGB(170, 0, 170, 0, 255))

            @Expose
            @ConfigOption(
                name = "§6Legendary §rColor",
                desc = "§7Default: §#§f§f§a§a§0§0§/#FFAA00"
            )
            @ConfigEditorColour
            val legendaryColor: Property<ChromaColour> = Property.of(ChromaColour.fromRGB(255, 170, 0, 0, 255))

            @Expose
            @ConfigOption(
                name = "§dMythic §rColor",
                desc = "§7Default: §#§f§f§5§5§f§f§/#FF55FF"
            )
            @ConfigEditorColour
            val mythicColor: Property<ChromaColour> = Property.of(ChromaColour.fromRGB(255, 85, 255, 0, 255))
        }
    }

    @Expose
    @ConfigOption(
        name = "Icon Scale",
        desc = "How large the icon should be - Default is 1.7\n" +
            "§ePet Icon must be enabled above."
    )
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 2.5f, minStep = 0.1f)
    val iconScale: Property<Double> = Property.of(1.7)

    @Expose
    @ConfigOption(
        name = "Skin Animation",
        desc = "If your pet has an animated skin, display the animated skin for the icon.\n" +
            "§ePet Icon must be enabled above."
    )
    @ConfigEditorBoolean
    val skinAnimation: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Icon Spin",
        desc = "Spin the pet icon in place.\n" +
            "§ePet Icon must be enabled above."
    )
    @ConfigEditorDropdown
    val spinDirection: Property<SpinDirection> = Property.of(SpinDirection.NONE)

    enum class SpinDirection(private val displayName: String) {
        NONE("No Spinning"),
        CLOCKWISE("Clockwise"),
        COUNTER_CLOCKWISE("Counter-Clockwise"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Spin Speed",
        desc = "How long in seconds it should take for one spin to complete.\n" +
            "§ePet Icon and §eIcon Spin must be enabled above."
    )
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 10f, minStep = 0.5f)
    val spinFrequency: Property<Float> = Property.of(2.0f)
}
