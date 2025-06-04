package at.hannibal2.skyhanni.config.features.pets.display.visual

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class VisualCustomizationConfig {

    @Expose
    @ConfigOption(name = "Separator Ring", desc = "")
    @Accordion
    val separatorRing: RingConfig = RingConfig()

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

    @Expose
    @ConfigOption(name = "Exp-Share Pets", desc = "")
    @Accordion
    val expSharePet: ExpSharePetConfig = ExpSharePetConfig()
}
