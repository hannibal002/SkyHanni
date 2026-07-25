package at.hannibal2.skyhanni.config.features.pets.display.visual

import at.hannibal2.skyhanni.config.features.pets.display.ResettableScalableConfig
import at.hannibal2.skyhanni.utils.LorenzRarity
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class RarityBackgroundConfig(
    override val scalar: Float = 1.0f,
) : ResettableScalableConfig {
    companion object {
        private const val DEFAULT_PADDING = 4f
    }

    fun getRarityBackgroundColor(rarity: LorenzRarity): ChromaColour = when (rarity) {
        LorenzRarity.COMMON -> commonColor.get()
        LorenzRarity.UNCOMMON -> uncommonColor.get()
        LorenzRarity.RARE -> rareColor.get()
        LorenzRarity.EPIC -> epicColor.get()
        LorenzRarity.LEGENDARY -> legendaryColor.get()
        LorenzRarity.MYTHIC -> mythicColor.get()
        else -> rarity.color.toChromaColor()
    }

    @Expose
    @ConfigOption(
        name = "Background Padding",
        desc = "How much extra padding should be added to the background circle."
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 8f, minStep = 0.25f)
    val padding: Property<Float> = Property.of(DEFAULT_PADDING * scalar)

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

    @ConfigOption(name = "Reset Colors", desc = "Reset colors to their default values.")
    @ConfigEditorButton(buttonText = "Reset")
    val reset: Runnable = Runnable(::reset)
}
