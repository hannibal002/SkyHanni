package at.hannibal2.skyhanni.config.features.pets.display.visual

import at.hannibal2.skyhanni.config.features.pets.display.ResettableScalableConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

/**
 * Config for visual elements of the pet display, such as the pet icon, background color, and held item display.
 *
 * @param scalar A scalar value that can be used to scale all visual elements of the pet display, for easier re-use of
 *  the config class.
 */
open class VisualPetDisplayConfig(
    scalar: Float = 1.0f,
) : ResettableScalableConfig {
    @Suppress("CanBePrimaryConstructorProperty")
    @Transient
    override val scalar: Float = scalar

    @Expose
    @ConfigOption(name = "Pet Icon", desc = "")
    @Accordion
    open val icon: IconConfig = IconConfig(scalar)

    @Expose
    @ConfigOption(name = "Background Color", desc = "")
    @Accordion
    open val rarityBackground: BackgroundColorConfig = BackgroundColorConfig(scalar)

    open class BackgroundColorConfig(
        scalar: Float = 1.0f,
    ) : ResettableScalableConfig {
        @Suppress("CanBePrimaryConstructorProperty")
        @Transient
        override val scalar: Float = scalar

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
        open val customization: RarityBackgroundConfig = RarityBackgroundConfig(scalar)

        @Expose
        @ConfigOption(name = "Border Ring", desc = "")
        @Accordion
        open val borderRing: BorderRingConfig = BorderRingConfig(scalar)
    }

    @Expose
    @ConfigOption(name = "Pet Item", desc = "")
    @Accordion
    open val petItem: PetItemConfig = PetItemConfig(scalar)
}
