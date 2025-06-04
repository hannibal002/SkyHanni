package at.hannibal2.skyhanni.config.features.pets.display.visual

import at.hannibal2.skyhanni.utils.renderables.OrbitDirection
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
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
    val icon: Property<Boolean> = Property.of(true)

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
    @ConfigOption(
        name = "EXP Share Pets",
        desc = "Adds additional pet icons for your pets active in Exp Share.\n" +
            "§cOpen the exp share menu if information is out of date."
    )
    @ConfigEditorBoolean
    val expSharePet: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Visual Customization", desc = "")
    @Accordion
    val customization: VisualCustomizationConfig = VisualCustomizationConfig()

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
