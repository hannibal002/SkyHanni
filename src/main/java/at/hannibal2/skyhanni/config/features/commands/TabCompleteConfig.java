package at.hannibal2.skyhanni.config.features.commands;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class TabCompleteConfig {

    @Expose
    @ConfigOption(name = "Warps", desc = "Tab complete the warp-point names when typing §e/warp <TAB>§7.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean warps = true;

    @Expose
    @ConfigOption(name = "Island Players", desc = "Tab complete other players on the same island.")
    public boolean islandPlayers = true;

    @Expose
    @ConfigOption(name = "Friends", desc = "Tab complete friends from your friends list.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean friends = true;

    @Expose
    @ConfigOption(name = "Only Best Friends", desc = "Only Tab Complete best friends.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean onlyBestFriends = false;

    @Expose
    @ConfigOption(name = "Party", desc = "Tab complete Party Members.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean party = true;

    @Expose
    @ConfigOption(name = "VIP Visits", desc = "Tab complete the visit to special users with cake souls on it.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean vipVisits = true;

    @Expose
    @ConfigOption(name = "/gfs Sack", desc = "Tab complete §e/gfs §7sack items.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean gfsSack = true;

    @Expose
    @ConfigOption(name = "Party Commands", desc = "Tab complete commonly used party commands.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean partyCommands = true;

    @Expose
    @ConfigOption(name = "View Recipe", desc = "Tab complete item IDs in the the Hypixel command §e/viewrecipe§7. Only items with recipes are tab completed.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean viewrecipeItems = true;
}
