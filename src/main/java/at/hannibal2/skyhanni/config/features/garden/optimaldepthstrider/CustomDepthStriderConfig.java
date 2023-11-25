package at.hannibal2.skyhanni.config.features.garden.optimaldepthstrider;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class CustomDepthStriderConfig {

    @Expose
    @ConfigOption(name = "Wheat", desc = "§eTODO: get a good description, explain 0=disabled")
    @ConfigEditorSlider(minValue = 0, maxValue = 3, minStep = 1)
    public int wheat = 0;

    @Expose
    @ConfigOption(name = "Carrot", desc = "§eTODO: get a good description, explain 0=disabled")
    @ConfigEditorSlider(minValue = 0, maxValue = 3, minStep = 1)
    public int carrot = 0;

    @Expose
    @ConfigOption(name = "Potato", desc = "§eTODO: get a good description, explain 0=disabled")
    @ConfigEditorSlider(minValue = 0, maxValue = 3, minStep = 1)
    public int potato = 0;

    @Expose
    @ConfigOption(name = "Nether Wart", desc = "§eTODO: get a good description, explain 0=disabled")
    @ConfigEditorSlider(minValue = 0, maxValue = 3, minStep = 1)
    public int netherWart = 0;

    @Expose
    @ConfigOption(name = "Pumpkin", desc = "§eTODO: get a good description, explain 0=disabled")
    @ConfigEditorSlider(minValue = 0, maxValue = 3, minStep = 1)
    public int pumpkin = 0;

    @Expose
    @ConfigOption(name = "Melon", desc = "§eTODO: get a good description, explain 0=disabled")
    @ConfigEditorSlider(minValue = 0, maxValue = 3, minStep = 1)
    public int melon = 0;

    @Expose
    @ConfigOption(name = "Cocoa Beans", desc = "§eTODO: get a good description, explain 0=disabled")
    @ConfigEditorSlider(minValue = 0, maxValue = 3, minStep = 1)
    public int cocoaBeans = 0;

    @Expose
    @ConfigOption(name = "Sugar Cane", desc = "§eTODO: get a good description, explain 0=disabled")
    @ConfigEditorSlider(minValue = 0, maxValue = 3, minStep = 1)
    public int sugarCane = 0;

    @Expose
    @ConfigOption(name = "Cactus", desc = "§eTODO: get a good description, explain 0=disabled")
    @ConfigEditorSlider(minValue = 0, maxValue = 3, minStep = 1)
    public int cactus = 0;

    @Expose
    @ConfigOption(name = "Mushroom", desc = "§eTODO: get a good description, explain 0=disabled")
    @ConfigEditorSlider(minValue = 0, maxValue = 3, minStep = 1)
    public int mushroom = 0;
}
