package at.hannibal2.skyhanni.config.features.garden.optimaldepthstrider;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class CustomDepthStriderConfig {

    @Expose
    @ConfigOption(name = "Wheat", desc = "Suggested:\n" +
        "§eTODO: get default depth strider value")
    @ConfigEditorSlider(minValue = 1, maxValue = 3, minStep = 1)
    public int wheat = 1;

    @Expose
    @ConfigOption(name = "Carrot", desc = "Suggested:\n" +
        "§eTODO: get default depth strider value")
    @ConfigEditorSlider(minValue = 1, maxValue = 3, minStep = 1)
    public int carrot = 1;

    @Expose
    @ConfigOption(name = "Potato", desc = "Suggested:\n" +
        "§eTODO: get default depth strider value")
    @ConfigEditorSlider(minValue = 1, maxValue = 3, minStep = 1)
    public int potato = 1;

    @Expose
    @ConfigOption(name = "Nether Wart", desc = "Suggested:\n" +
        "§eTODO: get default depth strider value")
    @ConfigEditorSlider(minValue = 1, maxValue = 3, minStep = 1)
    public int netherWart = 1;

    @Expose
    @ConfigOption(name = "Pumpkin", desc = "Suggested:\n" +
        "§eTODO: get default depth strider value")
    @ConfigEditorSlider(minValue = 1, maxValue = 3, minStep = 1)
    public int pumpkin = 1;

    @Expose
    @ConfigOption(name = "Melon", desc = "Suggested:\n" +
        "§eTODO: get default depth strider value")
    @ConfigEditorSlider(minValue = 1, maxValue = 3, minStep = 1)
    public int melon = 1;

    @Expose
    @ConfigOption(name = "Cocoa Beans", desc = "Suggested:\n" +
        "§eTODO: get default depth strider value")
    @ConfigEditorSlider(minValue = 1, maxValue = 3, minStep = 1)
    public int cocoaBeans = 1;

    @Expose
    @ConfigOption(name = "Sugar Cane", desc = "Suggested:\n" +
        "§eTODO: get default depth strider value")
    @ConfigEditorSlider(minValue = 1, maxValue = 3, minStep = 1)
    public int sugarCane = 1;

    @Expose
    @ConfigOption(name = "Cactus", desc = "Suggested:\n" +
        "§eTODO: get default depth strider value")
    @ConfigEditorSlider(minValue = 1, maxValue = 3, minStep = 1)
    public int cactus = 1;

    @Expose
    @ConfigOption(name = "Mushroom", desc = "Suggested:\n" +
        "§eTODO: get default depth strider value")
    @ConfigEditorSlider(minValue = 1, maxValue = 3, minStep = 1)
    public int mushroom = 1;
}
