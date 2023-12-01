package at.hannibal2.skyhanni.config.features.garden.optimalspeed;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class CustomSpeedConfig {

    @Expose
    @ConfigOption(name = "Wheat", desc = "Suggested farm speed:\n" +
        "§e5 Blocks§7: §f✦ 93 speed\n" +
        "§e4 Blocks§7: §f✦ 116 speed")
    @ConfigEditorSlider(minValue = 1, maxValue = 400, minStep = 1)
    public int wheat = 93;

    @Expose
    @ConfigOption(name = "Carrot", desc = "Suggested farm speed:\n" +
        "§e5 Blocks§7: §f✦ 93 speed\n" +
        "§e4 Blocks§7: §f✦ 116 speed")
    @ConfigEditorSlider(minValue = 1, maxValue = 400, minStep = 1)
    public int carrot = 93;

    @Expose
    @ConfigOption(name = "Potato", desc = "Suggested farm speed:\n" +
        "§e5 Blocks§7: §f✦ 93 speed\n" +
        "§e4 Blocks§7: §f✦ 116 speed")
    @ConfigEditorSlider(minValue = 1, maxValue = 400, minStep = 1)
    public int potato = 93;

    @Expose
    @ConfigOption(name = "Nether Wart", desc = "Suggested farm speed:\n" +
        "§e5 Blocks§7: §f✦ 93 speed\n" +
        "§e4 Blocks§7: §f✦ 116 speed")
    @ConfigEditorSlider(minValue = 1, maxValue = 400, minStep = 1)
    public int netherWart = 93;

    @Expose
    @ConfigOption(name = "Pumpkin", desc = "Suggested farm speed:\n" +
        "§e3 Blocks§7: §f✦ 155 speed\n" +
        "§e2 Blocks§7: §f✦ 265 §7or §f400 speed")
    @ConfigEditorSlider(minValue = 1, maxValue = 400, minStep = 1)
    public int pumpkin = 155;

    @Expose
    @ConfigOption(name = "Melon", desc = "Suggested farm speed:\n" +
        "§e3 Blocks§7: §f✦ 155 speed\n" +
        "§e2 Blocks§7: §f✦ 265 or 400 speed")
    @ConfigEditorSlider(minValue = 1, maxValue = 400, minStep = 1)
    public int melon = 155;

    @Expose
    @ConfigOption(name = "Cocoa Beans", desc = "Suggested farm speed:\n" +
        "§e3 Blocks§7: §f✦ 155 speed\n" +
        "§e4 Blocks§7: §f✦ 116 speed")
    @ConfigEditorSlider(minValue = 1, maxValue = 400, minStep = 1)
    public int cocoaBeans = 155;

    // TODO does other speed settings exist?
    @Expose
    @ConfigOption(name = "Sugar Cane", desc = "Suggested farm speed:\n" +
        "§eYaw 45§7: §f✦ 328 speed")
    @ConfigEditorSlider(minValue = 1, maxValue = 400, minStep = 1)
    public int sugarCane = 328;

    @Expose
    @ConfigOption(name = "Cactus", desc = "Suggested farm speed:\n" +
        "§eNormal§7: §f✦ 400 speed\n" +
        "§eRacing Helmet§7: §f✦ 464 speed\n" +
        "§eBlack Cat§7: §f✦ 464 speed")
    @ConfigEditorSlider(minValue = 1, maxValue = 500, minStep = 1)
    public int cactus = 400;

    // TODO does other speed settings exist?
    @Expose
    @ConfigOption(name = "Mushroom", desc = "Suggested farm speed:\n" +
        "§eYaw 60§7: §f✦ 233 speed")
    @ConfigEditorSlider(minValue = 1, maxValue = 400, minStep = 1)
    public int mushroom = 233;
}
