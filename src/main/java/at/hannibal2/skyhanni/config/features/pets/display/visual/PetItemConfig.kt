package at.hannibal2.skyhanni.config.features.pets.display.visual

import at.hannibal2.skyhanni.config.features.pets.display.ResettableScalableConfig
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class PetItemConfig(
    override val scalar: Float = 1.0f,
) : ResettableScalableConfig {
    companion object {
        private const val DEFAULT_ITEM_SCALE = 1.0f
    }

    @Expose
    @ConfigOption(
        name = "Enabled",
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

    enum class PetItemPlacement(val vertical: VerticalAlignment, val horizontal: HorizontalAlignment) {
        TOP_LEFT(VerticalAlignment.TOP, HorizontalAlignment.LEFT),
        TOP_CENTER(VerticalAlignment.TOP, HorizontalAlignment.CENTER),
        TOP_RIGHT(VerticalAlignment.TOP, HorizontalAlignment.RIGHT),
        CENTER_LEFT(VerticalAlignment.CENTER, HorizontalAlignment.LEFT),
        CENTER(VerticalAlignment.CENTER, HorizontalAlignment.CENTER),
        CENTER_RIGHT(VerticalAlignment.CENTER, HorizontalAlignment.RIGHT),
        BOTTOM_LEFT(VerticalAlignment.BOTTOM, HorizontalAlignment.LEFT),
        BOTTOM_CENTER(VerticalAlignment.BOTTOM, HorizontalAlignment.CENTER),
        BOTTOM_RIGHT(VerticalAlignment.BOTTOM, HorizontalAlignment.RIGHT),
        ;

        override fun toString() = "$vertical $horizontal"
    }

    @Expose
    @ConfigOption(
        name = "Item Scale",
        desc = "How large the pet item icon should be."
    )
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 2.0f, minStep = 0.1f)
    val scale: Property<Float> = Property.of(DEFAULT_ITEM_SCALE * scalar)
}
