package at.hannibal2.skyhanni.config.features.fishing.trophyfishing;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class ChatMessagesConfig {

    @Expose
    @ConfigOption(
        name = "Trophy Counter",
        desc = "Counts Trophy messages from chat and tells you how many you have found."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(
        name = "Trophy Counter Design",
        desc = "§fStyle 1: §72. §6§lGOLD §5Moldfin\n" +
            "§fStyle 2: §bYou caught a §5Moldfin §6§lGOLD§b. §7(2)\n" +
            "§fStyle 3: §bYou caught your 2nd §6§lGOLD §5Moldfin§b."
    )
    @ConfigEditorDropdown(values = {"Style 1", "Style 2", "Style 3"})
    public int design = 0;

    @Expose
    @ConfigOption(name = "Show Total Amount", desc = "Show total amount of all rarities at the end of the chat message.")
    @ConfigEditorBoolean
    public boolean totalAmount = false;

    @Expose
    @ConfigOption(name = "Trophy Fish Info", desc = "Show information and stats about a Trophy Fish when hovering over a catch message in chat.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean tooltip = true;

    @Expose
    @ConfigOption(name = "Hide Repeated Catches", desc = "Delete past catches of the same Trophy Fish from chat.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean duplicateHider = false;

    @Expose
    @ConfigOption(name = "Bronze Duplicates", desc = "Hide duplicate messages for bronze Trophy Fishes from chat.")
    @ConfigEditorBoolean
    public boolean bronzeHider = false;

    @Expose
    @ConfigOption(name = "Silver Duplicates", desc = "Hide duplicate messages for silver Trophy Fishes from chat.")
    @ConfigEditorBoolean
    public boolean silverHider = false;
}
