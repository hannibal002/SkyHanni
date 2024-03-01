package at.hannibal2.skyhanni.config.features.misc.compacttablist;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class CompactTabListConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Compacts the tablist to make it look much nicer like SBA did. Also " +
        "doesn't break god-pot detection and shortens some other lines.")
    //made tablist one word here so both searches will pick it up
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(
        name = "Toggle Tab",
        desc = "Use the tab key to toggle the tab list, not show tab list while the key is pressed. " +
            "Similar to patcher's feature."
    )
    @ConfigEditorBoolean
    public boolean toggleTab = false;

    @Expose
    @ConfigOption(name = "Hide Hypixel Adverts", desc = "Hides text from advertising the Hypixel server or store in the tablist.")
    @ConfigEditorBoolean
    public boolean hideAdverts = false;

    @Expose
    @ConfigOption(name = "Hide Fire Sale Adverts", desc = "Hides fire sales from the tablist")
    @ConfigEditorBoolean
    public boolean hideFiresales = false;

    @Expose
    @ConfigOption(name = "Advanced Player List", desc = "")
    @Accordion
    public AdvancedPlayerListConfig advancedPlayerList = new AdvancedPlayerListConfig();
}
