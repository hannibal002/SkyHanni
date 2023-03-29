package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class Bingo {

    @Expose
    @ConfigOption(name = "Compact Chat Messages", desc = "Shortens chat messages about skill level ups, collection gains, " +
            "new area discoveries, and bestiary upgrades while on bingo.")
    @ConfigEditorBoolean
    public boolean compactChatMessages = true;

    @Expose
    @ConfigOption(name = "Bingo Card View", desc = "Simply showing the bingo card. Toggle by sneaking with skyblock menu in hand.")
    @ConfigEditorBoolean
    public boolean cardDisplay = true;

    @Expose
    @ConfigOption(name = "Bingo Steps", desc = "Show help with the next step in bingo instead of the bingo card. " +
            "§cThis feature is in early development. Expect bugs and missing goals.")
    @ConfigEditorBoolean
    public boolean stepHelper = false;

    @Expose
    public Position bingoCardPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Minion Craft Helper", desc = "Show how many more items you need to upgrade the minion in your inventory. Especially useful for bingo.")
    @ConfigEditorBoolean
    public boolean minionCraftHelperEnabled = true;

    @Expose
    public Position minionCraftHelperPos = new Position(10, 10, false, true);
}
