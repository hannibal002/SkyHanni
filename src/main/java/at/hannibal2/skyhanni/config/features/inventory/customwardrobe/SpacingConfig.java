package at.hannibal2.skyhanni.config.features.inventory.customwardrobe;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class SpacingConfig {

    @Expose
    @ConfigOption(name = "globl skal", desc = "")
    @ConfigEditorSlider(
        minValue = 30,
        maxValue = 200,
        minStep = 1
    )
    public int globalScale = 100;

    @Expose
    @ConfigOption(name = "bordr thicc", desc = "")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 15,
        minStep = 1
    )
    public int outlineThickness = 5;

    @Expose
    @ConfigOption(name = "bordr blrrr", desc = "")
    @ConfigEditorSlider(
        minValue = 0f,
        maxValue = 1f,
        minStep = 0.1f
    )
    public float outlineBlur = 0.5f;

    @Expose
    @ConfigOption(name = "slot widt", desc = "")
    @ConfigEditorSlider(
        minValue = 30,
        maxValue = 100,
        minStep = 1
    )
    public int slotWidth = 75;

    @Expose
    @ConfigOption(name = "slot haiiight", desc = "")
    @ConfigEditorSlider(
        minValue = 60,
        maxValue = 200,
        minStep = 1
    )
    public int slotHeight = 140;

    @Expose
    @ConfigOption(name = "people big or nuhuh", desc = "")
    @ConfigEditorSlider(
        minValue = 0,
        maxValue = 100,
        minStep = 1
    )
    public int playerScale = 75;

    @Expose
    @ConfigOption(name = "no mor people in ma row >:(", desc = "")
    @ConfigEditorSlider(
        minValue = 5,
        maxValue = 18,
        minStep = 1
    )
    public int maxPlayersPerRow = 9;

    @Expose
    @ConfigOption(name = "how wide bwetween pweopole", desc = "")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 20,
        minStep = 1
    )
    public int horizontalSpacing = 3;

    @Expose
    @ConfigOption(name = "how tall bwetween pweopole", desc = "")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 20,
        minStep = 1
    )
    public int verticalSpacing = 3;

    @Expose
    @ConfigOption(name = "how tall between people and button!", desc = "")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 40,
        minStep = 1
    )
    public int buttonSlotsVerticalSpacing = 10;

    @Expose
    @ConfigOption(name = "how wide between button!", desc = "")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 40,
        minStep = 1
    )
    public int buttonHorizontalSpacing = 10;

    @Expose
    @ConfigOption(name = "how tall between button!", desc = "")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 40,
        minStep = 1
    )
    public int buttonVerticalSpacing = 10;

    @Expose
    @ConfigOption(name = "how thicc button?", desc = "")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 60,
        minStep = 1
    )
    public int buttonWidth = 50;

    @Expose
    @ConfigOption(name = "how tal button?", desc = "")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 60,
        minStep = 1
    )
    public int buttonHeight = 20;

}
