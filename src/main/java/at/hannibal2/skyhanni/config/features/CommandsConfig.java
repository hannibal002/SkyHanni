package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class CommandsConfig {

    @ConfigOption(name = "Tab Complete", desc = "")
    @Accordion
    @Expose
    public TabCompleteConfig tabComplete = new TabCompleteConfig();

    public static class TabCompleteConfig {

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
    }

    @Expose
    @ConfigOption(name = "Fandom Wiki", desc = "Use Fandom wiki (§ehypixel-skyblock.fandom.com§7) instead of the Hypixel wiki (§ewiki.hypixel.net§7).")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean useFandomWiki = false;

    @ConfigOption(name = "Party Commands", desc = "Shortens party commands and allows tab-completing for them. " +
        "\n§eCommands: /pt /pp /pko /pk §7SkyBlock command §e/pt §7to check the play time still works.")
    @Expose
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean shortCommands = true;

    @Expose
    @ConfigOption(name = "Replace Warp Is", desc = "Adds §e/warp is §7alongside §e/is§7. Idk why. Ask §cKaeso")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean replaceWarpIs = false;

    @Expose
    @ConfigOption(name = "/viewrecipe Lower Case", desc = "Adds support for lower case item IDs to the Hypixel command §e/viewrecipe§7.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean viewRecipeLowerCase = true;
}
