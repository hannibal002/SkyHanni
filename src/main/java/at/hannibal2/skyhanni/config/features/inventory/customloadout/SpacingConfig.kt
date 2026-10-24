package at.hannibal2.skyhanni.config.features.inventory.customloadout

import at.hannibal2.skyhanni.config.storage.Resettable
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SpacingConfig : Resettable {

    @ConfigOption(name = "Reset to Default", desc = "Reset all spacing settings to the default.")
    @ConfigEditorButton(buttonText = "Reset")
    val resetSpacing: Runnable = Runnable(::reset)

    @Expose
    @ConfigOption(name = "Global Scale", desc = "Control the scale of the entirety of the GUI.")
    @ConfigEditorSlider(minValue = 30f, maxValue = 200f, minStep = 1f)
    var globalScale: Int = 100

    @Expose
    @ConfigOption(name = "Outline Thickness", desc = "How thick the outline of the hovered slot is.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 15f, minStep = 1f)
    var outlineThickness: Int = 5

    @Expose
    @ConfigOption(name = "Outline Blur", desc = "Amount of blur of the outline.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 1f, minStep = 0.1f)
    var outlineBlur: Float = 0.5f

    @Expose
    @ConfigOption(name = "Slot Width", desc = "Width of the slots.")
    @ConfigEditorSlider(minValue = 30f, maxValue = 100f, minStep = 1f)
    var slotWidth: Int = 75

    @Expose
    @ConfigOption(name = "Slot Height", desc = "Height of the slots.")
    @ConfigEditorSlider(minValue = 60f, maxValue = 200f, minStep = 1f)
    var slotHeight: Int = 140

    @Expose
    @ConfigOption(name = "Player Scale", desc = "Scale of the players.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
    var playerScale: Int = 75

    @Expose
    @ConfigOption(name = "Slots Horizontal Spacing", desc = "How much space horizontally between slots.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 1f)
    var horizontalSpacing: Int = 3

    @Expose
    @ConfigOption(name = "Slots Vertical Spacing", desc = "How much space vertically between slots.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 1f)
    var verticalSpacing: Int = 3

    @Expose
    @ConfigOption(
        name = "Slots & Buttons Spacing",
        desc = "How much vertical space there is between slots and the buttons."
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 40f, minStep = 1f)
    var buttonSlotsVerticalSpacing: Int = 10

    @Expose
    @ConfigOption(name = "Button Horizontal Spacing", desc = "How much space horizontally between buttons.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 40f, minStep = 1f)
    var buttonHorizontalSpacing: Int = 10

    @Expose
    @ConfigOption(name = "Button Vertical Spacing", desc = "How much space vertically between buttons.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 40f, minStep = 1f)
    var buttonVerticalSpacing: Int = 10

    @Expose
    @ConfigOption(name = "Button Width", desc = "Width of the buttons.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 60f, minStep = 1f)
    var buttonWidth: Int = 50

    @Expose
    @ConfigOption(name = "Button Height", desc = "Height of the buttons.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 60f, minStep = 1f)
    var buttonHeight: Int = 20

    @Expose
    @ConfigOption(name = "Background Padding", desc = "Space between the edges of the background and the slots.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 1f)
    var backgroundPadding: Int = 10
}
