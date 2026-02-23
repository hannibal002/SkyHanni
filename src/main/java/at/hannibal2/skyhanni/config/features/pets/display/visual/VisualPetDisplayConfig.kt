package at.hannibal2.skyhanni.config.features.pets.display.visual

import at.hannibal2.skyhanni.config.storage.Resettable
import at.hannibal2.skyhanni.utils.RenderUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

open class VisualPetDisplayConfig : Resettable {
    @Expose
    @ConfigOption(name = "Pet Icon", desc = "")
    @Accordion
    open val icon: IconConfig = IconConfig()

    @Expose
    @ConfigOption(name = "Background Color", desc = "")
    @Accordion
    open val rarityBackground: BackgroundColorConfig = BackgroundColorConfig()

    open class BackgroundColorConfig {
        @Expose
        @ConfigOption(
            name = "Enabled",
            desc = "Display a background color around your pet.\n" +
            "Default is to display the rarity color of the pet."
        )
        @ConfigEditorBoolean
        open val enabled: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(name = "Color Customization", desc = "")
        @Accordion
        open val customization: RarityBackgroundConfig = RarityBackgroundConfig()

        @Expose
        @ConfigOption(name = "Border Ring", desc = "")
        @Accordion
        open val borderRing: BorderRingConfig = BorderRingConfig()
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
}
