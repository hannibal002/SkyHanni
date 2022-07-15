package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigEditorBoolean;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigOption;
import com.google.gson.annotations.Expose;

public class Fishing {

    @Expose
    @ConfigOption(name = "Trophy Counter", desc = "Counts every single Trohy message from chat and tells you how many you got already.")
    @ConfigEditorBoolean
    public boolean trophyCounter = false;

    @Expose
    @ConfigOption(name = "Hide Bronze Duplicates", desc = "Hide duplicate messages for bronze trophy fishes from chat.")
    @ConfigEditorBoolean
    public boolean trophyFishBronzeHider = false;
}