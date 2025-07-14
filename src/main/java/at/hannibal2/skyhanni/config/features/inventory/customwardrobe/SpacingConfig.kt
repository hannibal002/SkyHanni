package at.hannibal2.skyhanni.config.features.inventory.customwardrobe

import at.hannibal2.skyhanni.config.storage.ResettableStorageSet
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class SpacingConfig : ResettableStorageSet() {

    @ConfigOption(name = "Reset to Default", desc = "Reset all custom wardrobe spacing settings to the default.")
    @ConfigEditorButton(buttonText = "Reset")
    val resetSpacing: Runnable = Runnable(::reset)

    @Expose
    @ConfigOption(name = "Global Scale", desc = "Control the scale of the entirety of the wardrobe.")
    @ConfigEditorSlider(minValue = 30f, maxValue = 200f, minStep = 1f)
    val globalScale: Property<Int> = Property.of(100)

    @Expose
    @ConfigOption(name = "Outline Thickness", desc = "How thick the outline of the hovered slot is.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 15f, minStep = 1f)
    val outlineThickness: Property<Int> = Property.of(5)

    @Expose
    @ConfigOption(name = "Outline Blur", desc = "Amount of blur of the outline.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 1f, minStep = 0.1f)
    val outlineBlur: Property<Float> = Property.of(0.5f)

    @Expose
    @ConfigOption(name = "Slot Width", desc = "Width of the wardrobe slots.")
    @ConfigEditorSlider(minValue = 30f, maxValue = 100f, minStep = 1f)
    val slotWidth: Property<Int> = Property.of(75)

    @Expose
    @ConfigOption(name = "Slot Height", desc = "Height of the wardrobe slots.")
    @ConfigEditorSlider(minValue = 60f, maxValue = 200f, minStep = 1f)
    val slotHeight: Property<Int> = Property.of(140)

    @Expose
    @ConfigOption(name = "Player Scale", desc = "Scale of the players.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
    val playerScale: Property<Int> = Property.of(75)

    @Expose
    @ConfigOption(name = "Slots per Row", desc = "Max amount of wardrobe slots per row.")
    @ConfigEditorSlider(minValue = 5f, maxValue = 18f, minStep = 1f)
    val maxPlayersPerRow: Property<Int> = Property.of(9)

    @Expose
    @ConfigOption(name = "Slots Horizontal Spacing", desc = "How much space horizontally between wardrobe slots.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 1f)
    val horizontalSpacing: Property<Int> = Property.of(3)

    @Expose
    @ConfigOption(name = "Slots Vertical Spacing", desc = "How much space vertically between wardrobe slots.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 1f)
    val verticalSpacing: Property<Int> = Property.of(3)

    @Expose
    @ConfigOption(
        name = "Slots & Buttons Spacing",
        desc = "How much vertical space there is between wardrobe slots and the buttons."
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 40f, minStep = 1f)
    val buttonSlotsVerticalSpacing: Property<Int> = Property.of(10)

    @Expose
    @ConfigOption(name = "Button Horizontal Spacing", desc = "How much space horizontally between buttons.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 40f, minStep = 1f)
    val buttonHorizontalSpacing: Property<Int> = Property.of(10)

    @Expose
    @ConfigOption(name = "Button Vertical Spacing", desc = "How much space vertically between buttons.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 40f, minStep = 1f)
    val buttonVerticalSpacing: Property<Int> = Property.of(10)

    @Expose
    @ConfigOption(name = "Button Width", desc = "Width of the buttons.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 60f, minStep = 1f)
    val buttonWidth: Property<Int> = Property.of(50)

    @Expose
    @ConfigOption(name = "Button Height", desc = "Height of the buttons.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 60f, minStep = 1f)
    val buttonHeight: Property<Int> = Property.of(20)

    @Expose
    @ConfigOption(name = "Background Padding", desc = "Space between the edges of the background and the slots.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 1f)
    val backgroundPadding: Property<Int> = Property.of(10)
}
