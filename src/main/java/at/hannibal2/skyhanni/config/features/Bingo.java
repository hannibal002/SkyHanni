package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class Bingo {

    @Expose
    @ConfigOption(name = "Bingo Card", desc = "")
    @Accordion
    public BingoCard bingoCard = new BingoCard();

    public static class BingoCard {
        @Expose
        @ConfigOption(name = "Enable", desc = "Displays the bingo card. Toggle by sneaking with SkyBlock menu in hand.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Bingo Steps", desc = "Show help with the next step in bingo instead of the bingo card. " +
                "§cThis feature is in early development. Expect bugs and missing goals.")
        @ConfigEditorBoolean
        public boolean stepHelper = false;

        @Expose
        public Position bingoCardPos = new Position(10, 10, false, true);
    }

    @Expose
    @ConfigOption(name = "Compact Chat Messages", desc = "")
    @Accordion
    public CompactChat compactChat = new CompactChat();

    public static class CompactChat {

        @Expose
        @ConfigOption(name = "Enable", desc = "Shortens chat messages about skill level ups, collection gains, " +
                "new area discoveries, bestiary upgrades and skyblock level up messages while on bingo.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Hide Border", desc = "Hide the border messages before and after the compact level up messages.")
        @ConfigEditorBoolean
        public boolean hideBorder = true;

        @Expose
        @ConfigOption(name = "Outside Bingo", desc = "Compact the level up chat messages outside of an bingo profile as well.")
        @ConfigEditorBoolean
        public boolean outsideBingo = false;
    }

    @Expose
    @ConfigOption(name = "Minion Craft Helper", desc = "Show how many more items you need to upgrade the minion in your inventory. Especially useful for bingo.")
    @ConfigEditorBoolean
    public boolean minionCraftHelperEnabled = true;

    @Expose
    public Position minionCraftHelperPos = new Position(10, 10, false, true);
}
