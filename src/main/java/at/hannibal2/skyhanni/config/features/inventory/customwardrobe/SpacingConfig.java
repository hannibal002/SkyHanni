package at.hannibal2.skyhanni.config.features.inventory.customwardrobe;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class SpacingConfig {

    @Expose
    @ConfigOption(name = "Global Scale", desc = "Controls the scale of the entirety of the wardrobe.")
    @ConfigEditorSlider(
        minValue = 30,
        maxValue = 200,
        minStep = 1
    )
    public int globalScale = 100;

    @Expose
    @ConfigOption(name = "Outline Thickness", desc = "How thick the outline of the hovered slot is.")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 15,
        minStep = 1
    )
    public int outlineThickness = 5;

    @Expose
    @ConfigOption(name = "Outline Blur", desc = "Amount of blur of the outline.")
    @ConfigEditorSlider(
        minValue = 0f,
        maxValue = 1f,
        minStep = 0.1f
    )
    public float outlineBlur = 0.5f;

    @Expose
    @ConfigOption(name = "Slot Width", desc = "Width of the wardrobe slots.")
    @ConfigEditorSlider(
        minValue = 30,
        maxValue = 100,
        minStep = 1
    )
    public int slotWidth = 75;

    @Expose
    @ConfigOption(name = "Slot Height", desc = "Height of the wardrobe slots.")
    @ConfigEditorSlider(
        minValue = 60,
        maxValue = 200,
        minStep = 1
    )
    public int slotHeight = 140;

    @Expose
    @ConfigOption(name = "Player Scale", desc = "Scale of the players.")
    @ConfigEditorSlider(
        minValue = 0,
        maxValue = 100,
        minStep = 1
    )
    public int playerScale = 75;

    @Expose
    @ConfigOption(name = "Slots per Row", desc = "Max amount of wardrobe slots per row.")
    @ConfigEditorSlider(
        minValue = 5,
        maxValue = 18,
        minStep = 1
    )
    public int maxPlayersPerRow = 9;

    @Expose
    @ConfigOption(name = "Slots Horizontal Spacing", desc = "How much space horizontally between wardrobe slots.")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 20,
        minStep = 1
    )
    public int horizontalSpacing = 3;

    @Expose
    @ConfigOption(name = "Slots Vertical Spacing", desc = "How much space vertically between wardrobe slots.")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 20,
        minStep = 1
    )
    public int verticalSpacing = 3;

    @Expose
    @ConfigOption(name = "Slots & Buttons Spacing", desc = "How much vertical space there is between wardrobe slots and the buttons.")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 40,
        minStep = 1
    )
    public int buttonSlotsVerticalSpacing = 10;

    @Expose
    @ConfigOption(name = "Button Horizontal Spacing", desc = "How much space horizontally between buttons.")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 40,
        minStep = 1
    )
    public int buttonHorizontalSpacing = 10;

    @Expose
    @ConfigOption(name = "Button Vertical Spacing", desc = "How much space vertically between buttons.")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 40,
        minStep = 1
    )
    public int buttonVerticalSpacing = 10;

    @Expose
    @ConfigOption(name = "Button Width", desc = "Width of the buttons.")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 60,
        minStep = 1
    )
    public int buttonWidth = 50;

    @Expose
    @ConfigOption(name = "Button Height", desc = "Height of the buttons.")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 60,
        minStep = 1
    )
    public int buttonHeight = 20;

}
