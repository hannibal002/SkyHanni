package at.hannibal2.skyhanni.config.features.pets.display.visual

import at.hannibal2.skyhanni.config.features.pets.display.ResettableScalableConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.ConfigOrder
import io.github.notenoughupdates.moulconfig.annotations.ConfigOverride
import io.github.notenoughupdates.moulconfig.observer.Property

open class BorderRingConfig(
    override val scalar: Float = 1.0f,
) : ResettableScalableConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Display a border ring around the background color, showing your pet's progress to leveling up.",
    )
    @ConfigEditorBoolean
    @ConfigOrder(10)
    open val enabled: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Customization", desc = "")
    @Accordion
    @ConfigOrder(20)
    val customization: XPRingConfig = XPRingConfig(scalar)

    @Expose
    @ConfigOption(name = "Separator Ring", desc = "")
    @Accordion
    @ConfigOrder(30)
    open val separator: SeparatorRingConfig = SeparatorRingConfig(scalar)

    class SeparatorRingConfig(
        scalar: Float = 1.0f,
    ) : RingConfig(scalar) {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Display a separator ring between the background and the XP ring.")
        @ConfigEditorBoolean
        @ConfigOrder(10)
        val enabled: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(
            name = "Ring Color",
            desc = "The color of the ring.\n" +
                "§7Default: §#§8§0§8§0§8§0§/#808080"
        )
        @ConfigEditorColour
        @ConfigOverride
        @ConfigOrder(20)
        override val color: Property<ChromaColour> = Property.of(DEFAULT_RING_COLOR)
    }

    @ConfigOption(name = "Reset Ring Settings", desc = "Reset border ring settings to the default values.")
    @ConfigEditorButton(buttonText = "Reset")
    @ConfigOrder(40)
    val reset: Runnable = Runnable(::reset)
}

private val DEFAULT_XP_FILLED_COLOR = ChromaColour.fromRGB(0, 255, 255, 0, 255)
private val DEFAULT_XP_UNFILLED_COLOR = ChromaColour.fromRGB(192, 192, 192, 0, 255)

class XPRingConfig(
    scalar: Float = 1.0f,
) : RingConfig(scalar) {

    override val color: Property<ChromaColour> get() = filledColor

    @Expose
    @ConfigOption(
        name = "Filled Ring Color",
        desc = "The color of the filled portion of the ring.\n" +
            "§7Default: §#§0§0§f§f§f§f§/#00FFFF",
    )
    @ConfigEditorColour
    @ConfigOrder(21)
    val filledColor: Property<ChromaColour> = Property.of(DEFAULT_XP_FILLED_COLOR)

    @Expose
    @ConfigOption(
        name = "Unfilled Ring Color",
        desc = "The color of the unfilled portion of the ring.\n" +
            "§7Default: §#§c§0§c§0§c§0§/#C0C0C0",
    )
    @ConfigEditorColour
    @ConfigOrder(22)
    val unfilledColor: Property<ChromaColour> = Property.of(DEFAULT_XP_UNFILLED_COLOR)

    @ConfigOption(name = "Reset", desc = "Reset XP ring settings to the default values.")
    @ConfigEditorButton(buttonText = "Reset")
    @ConfigOverride
    override val reset: Runnable = Runnable(::reset)
}

open class RingConfig(
    scalar: Float = 1.0f,
) : ResettableScalableConfig {
    companion object {
        @Transient
        internal val DEFAULT_RING_COLOR = ChromaColour.fromRGB(128, 128, 128, 0, 255)
        private const val DEFAULT_PADDING = 3f
    }

    @Suppress("CanBePrimaryConstructorProperty")
    @Transient
    override val scalar: Float = scalar

    @Expose
    @ConfigOption(
        name = "Ring Padding",
        desc = "How wide the ring should be."
    )
    @ConfigEditorSlider(minValue = 2f, maxValue = 10f, minStep = 0.5f)
    @ConfigOrder(10)
    val padding: Property<Float> = Property.of(DEFAULT_PADDING * scalar)

    @Transient
    open val color: Property<ChromaColour> = Property.of(DEFAULT_RING_COLOR)

    @ConfigOption(name = "Reset Ring Settings", desc = "Reset the ring settings to the default values.")
    @ConfigEditorButton(buttonText = "Reset")
    @ConfigOrder(30)
    open val reset: Runnable = Runnable(::reset)
}
