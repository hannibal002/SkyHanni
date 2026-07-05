package at.hannibal2.skyhanni.config.features.pets.display.visual

import at.hannibal2.skyhanni.config.features.pets.display.ResettableScalableConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.ConfigOrder
import io.github.notenoughupdates.moulconfig.observer.Property

open class VisualPetDisplayConfig(
    override val scalar: Float = 1.0f,
) : ResettableScalableConfig {

    @Expose
    @ConfigOption(name = "Pet Icon", desc = "")
    @Accordion
    @ConfigOrder(10)
    open val icon: IconConfig = IconConfig(scalar)

    @Expose
    @ConfigOption(name = "Background Color", desc = "")
    @Accordion
    @ConfigOrder(20)
    open val rarityBackground: BackgroundColorConfig = BackgroundColorConfig(scalar)

    open class BackgroundColorConfig(
        override val scalar: Float = 1.0f,
    ) : ResettableScalableConfig {

        @Expose
        @ConfigOption(
            name = "Enabled",
            desc = "Display a background color around your pet.\n" +
                "Default is to display the rarity color of the pet."
        )
        @ConfigEditorBoolean
        @ConfigOrder(10)
        val enabled: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(name = "Customization", desc = "")
        @Accordion
        @ConfigOrder(20)
        val customization: RarityBackgroundConfig = RarityBackgroundConfig(scalar)

        @Expose
        @ConfigOption(name = "Border Ring", desc = "")
        @Accordion
        @ConfigOrder(30)
        open val borderRing: BorderRingConfig = BorderRingConfig(scalar)
    }

    @Expose
    @ConfigOption(name = "Pet Item", desc = "")
    @Accordion
    @ConfigOrder(30)
    open val petItem: PetItemConfig = PetItemConfig(scalar)
}
