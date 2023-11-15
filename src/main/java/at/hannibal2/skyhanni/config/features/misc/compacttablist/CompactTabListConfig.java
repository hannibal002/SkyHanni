package at.hannibal2.skyhanni.config.features.misc.compacttablist;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
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
    @ConfigOption(name = "Hide Hypixel Adverts", desc = "Hides text from advertising the Hypixel server or store in the tablist.")
    @ConfigEditorBoolean
    public boolean hideAdverts = false;

    @Expose
    @ConfigOption(name = "Advanced Player List", desc = "")
    @Accordion
    public AdvancedPlayerListConfig advancedPlayerList = new AdvancedPlayerListConfig();
}
