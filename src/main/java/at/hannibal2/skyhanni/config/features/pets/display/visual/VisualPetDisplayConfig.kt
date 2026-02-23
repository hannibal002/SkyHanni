package at.hannibal2.skyhanni.config.features.pets.display.visual

import at.hannibal2.skyhanni.config.storage.Resettable
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.renderables.animated.OrbitDirection
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

open class VisualPetDisplayConfig : Resettable {
    @Expose
    @ConfigOption(name = "Pet Icon", desc = "")
    @Accordion
    val icon: IconConfig = IconConfig()

    @Expose
    @ConfigOption(name = "Background Color", desc = "")
    @Accordion
    val rarityBackground: BackgroundColorConfig = BackgroundColorConfig()

    open class BackgroundColorConfig {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Display a background color around your pet.\n" +
            "Default is to display the rarity color of the pet.")
        @ConfigEditorBoolean
        open val enabled: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(name = "Customization", desc = "")
        @Accordion
        open val customization: RarityBackgroundConfig = RarityBackgroundConfig()
    }

    @Expose
    @ConfigOption(name = "Pet Item", desc = "")
    @Accordion
    val petItem: PetItemConfig = PetItemConfig()

    class PetItemConfig {
        @Expose
        @ConfigOption(
            name = "Pet Item",
            desc = "Display the pet's held item as an itemstack.",
        )
        @ConfigEditorBoolean
        val enabled: Property<Boolean> = Property.of(false)

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
            TOP_CENTER(RenderUtils.VerticalAlignment.TOP, RenderUtils.HorizontalAlignment.CENTER),
            TOP_RIGHT(RenderUtils.VerticalAlignment.TOP, RenderUtils.HorizontalAlignment.RIGHT),
            CENTER_LEFT(RenderUtils.VerticalAlignment.CENTER, RenderUtils.HorizontalAlignment.LEFT),
            CENTER(RenderUtils.VerticalAlignment.CENTER, RenderUtils.HorizontalAlignment.CENTER),
            CENTER_RIGHT(RenderUtils.VerticalAlignment.CENTER, RenderUtils.HorizontalAlignment.RIGHT),
            BOTTOM_LEFT(RenderUtils.VerticalAlignment.BOTTOM, RenderUtils.HorizontalAlignment.LEFT),
            BOTTOM_CENTER(RenderUtils.VerticalAlignment.BOTTOM, RenderUtils.HorizontalAlignment.CENTER),
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
        val scale: Property<Double> = Property.of(1.0)
    }

    @Expose
    @ConfigOption(name = "XP Ring", desc = "")
    @Accordion
    open val borderRing: XPBorderRingConfig = XPBorderRingConfig()

    open class XPBorderRingConfig {
        @Expose
        @ConfigOption(
            name = "Enabled",
            desc = "Display a border ring around the background color, showing your pet's progress to leveling up.",
        )
        @ConfigEditorBoolean
        val enabled: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(name = "Customization", desc = "")
        @Accordion
        open val customization: XPRingConfig = XPRingConfig()

        open class RingConfig : Resettable {
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
            open val color: Property<ChromaColour> = Property.of(ChromaColour.fromRGB(128, 128, 128, 0, 255))
        }

        class XPRingConfig : RingConfig() {
            @Expose
            @ConfigOption(
                name = "Filled Ring Color",
                desc = "The color of the filled portion of the ring.\n" +
                    "§7Default: §#§0§0§f§f§f§f§/#00FFFF",
            )
            @ConfigEditorColour
            val filledColor: Property<ChromaColour> = Property.of(ChromaColour.fromRGB(0, 255, 255, 0, 255))
            override val color: Property<ChromaColour> get() = filledColor

            @Expose
            @ConfigOption(
                name = "Unfilled Ring Color",
                desc = "The color of the unfilled portion of the ring.\n" +
                    "§7Default: §#§c§0§c§0§c§0§/#C0C0C0",
            )
            @ConfigEditorColour
            val unfilledColor: Property<ChromaColour> = Property.of(ChromaColour.fromRGB(192, 192, 192, 0, 255))

            @ConfigOption(name = "Reset Colors", desc = "Reset the colors to the default values.")
            @ConfigEditorButton(buttonText = "Reset")
            val reset: Runnable = Runnable(::reset)
        }
    }
}
