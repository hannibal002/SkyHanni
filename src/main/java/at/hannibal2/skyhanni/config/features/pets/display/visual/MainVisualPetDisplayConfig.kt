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



open class MainVisualPetDisplayConfig : VisualPetDisplayConfig() {

    @Expose
    @ConfigOption(name = "XP Ring", desc = "")
    @Accordion
    val xpRing: XPRingConfig = XPRingConfig()

    class XPRingConfig : Resettable {
        @Expose
        @ConfigOption(
            name = "Enabled",
            desc = "Display a ring around the background color, showing your pet's progress to next level.",
        )
        @ConfigEditorBoolean
        val enabled: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(name = "XP Ring Customization", desc = "")
        @Accordion
        val xpRingCustomization: XpRingCustomizationConfig = XpRingCustomizationConfig()

        @Expose
        @ConfigOption(name = "Separator Ring", desc = "")
        @ConfigEditorBoolean
        val separatorRing: Property<Boolean> = Property.of(false)
    }

    @Expose
    @ConfigOption(name = "Separator Ring Customization", desc = "")
    @Accordion
    val separatorRingCustomization: RingConfig = RingConfig()

    @Expose
    @ConfigOption(
        name = "Exp-Share Pets",
        desc = "Adds additional pet icons for your pets active in Exp Share.\n" +
            "§cOpen the exp share menu if information is out of date."
    )
    @ConfigEditorBoolean
    val expSharePet: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Exp-Share Pets Customization", desc = "")
    @Accordion
    val expSharePetCustomization: ExpSharePetConfig = ExpSharePetConfig()

    @Expose
    @ConfigOption(name = "Icon Scale", desc = "How large the icon should be - Default is 2.0")
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 6.0f, minStep = 0.1f)
    val iconScale: Property<Double> = Property.of(2.0)
}
