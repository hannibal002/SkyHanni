package at.hannibal2.skyhanni.config.features.misc.compacttablist;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class AdvancedPlayerListConfig {

    @Expose
    @ConfigOption(name = "Player Sort", desc = "Change the sort order of player names in the tab list.")
    @ConfigEditorDropdown(values = {"Rank (Default)", "SB Level", "Name (Abc)", "Ironman/Bingo", "Party/Friends/Guild", "Random"})
    public int playerSortOrder = 0;

    @Expose
    @ConfigOption(name = "Invert Sort", desc = "Flip the player list order on its head (also works with default rank).")
    @ConfigEditorBoolean
    public boolean reverseSort = false;

    @Expose
    @ConfigOption(name = "Hide Player Icons", desc = "Hide the icons/skins of player in the tab list.")
    @ConfigEditorBoolean
    public boolean hidePlayerIcons = false;

    @Expose
    @ConfigOption(name = "Hide Rank Color", desc = "Hide the player rank color.")
    @ConfigEditorBoolean
    public boolean hideRankColor = false;

    @Expose
    @ConfigOption(name = "Hide Emblems", desc = "Hide the emblems behind the player name.")
    @ConfigEditorBoolean
    public boolean hideEmblem = false;

    @Expose
    @ConfigOption(name = "Hide Level", desc = "Hide the SkyBlock level numbers.")
    @ConfigEditorBoolean
    public boolean hideLevel = false;

    @Expose
    @ConfigOption(name = "Hide Level Brackets", desc = "Hide the gray brackets in front of and behind the level numbers.")
    @ConfigEditorBoolean
    public boolean hideLevelBrackets = false;

    @Expose
    @ConfigOption(name = "Level Color As Name", desc = "Use the color of the SkyBlock level for the player color.")
    @ConfigEditorBoolean
    public boolean useLevelColorForName = false;

    @Expose
    @ConfigOption(name = "Bingo Rank Number", desc = "Show the number of the bingo rank next to the icon. Useful if you are not so familar with bingo.")
    @ConfigEditorBoolean
    public boolean showBingoRankNumber = false;

    @Expose
    @ConfigOption(name = "Hide Factions", desc = "Hide the icon of the Crimson Isle Faction in the tab list.")
    @ConfigEditorBoolean
    public boolean hideFactions = false;

    @Expose
    @ConfigOption(name = "Mark Special Persons", desc = "Show special icons behind the name of guild members, party members, friends, and marked players.")
    @ConfigEditorBoolean
    public boolean markSpecialPersons = false;

    @Expose
    @ConfigOption(
        name = "Mark SkyHanni Devs",
        desc = "Adds a §c:O §7behind the tablist name of §cSkyHanni's contributors§7. " +
            "§eThose are the folks that coded the mod for you for free :)"
    )
    @ConfigEditorBoolean
    public boolean markSkyHanniContributors = false;
}
