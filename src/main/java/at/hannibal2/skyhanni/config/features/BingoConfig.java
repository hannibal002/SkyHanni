package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class BingoConfig {

    @Expose
    @ConfigOption(name = "Bingo Card", desc = "")
    @Accordion
    public BingoCard bingoCard = new BingoCard();

    public static class BingoCard {
        @Expose
        @ConfigOption(name = "Enable", desc = "Displays the bingo card.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = true;
        @Expose
        @ConfigOption(name = "Quick Toggle", desc = "Quickly toggle the bingo card or the step helper by sneaking with SkyBlock menu in hand.")
        @ConfigEditorBoolean
        public boolean quickToggle = true;

        @Expose
        @ConfigOption(name = "Bingo Steps", desc = "Show help with the next step in bingo instead of the bingo card. " +
                "§cThis feature is in early development. Expect bugs and missing goals.")
        @ConfigEditorBoolean
        public boolean stepHelper = false;

        @Expose
        @ConfigOption(name = "Hide Community Goals", desc = "Hide Community Goals from the Bingo Card display.")
        @ConfigEditorBoolean
        public Property<Boolean> hideCommunityGoals = Property.of(false);

        @Expose
        @ConfigOption(
                name = "Show Guide",
                desc = "Show tips and difficulty for bingo goals inside the bingo card inventory.\n" +
                        "These tips are made from inspirations and guides from the community,\n"+
                        "aiming to help you to complete the bingo card."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean bingoSplashGuide = true;

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
                "new area discoveries and skyblock level up messages while on bingo.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Hide Border", desc = "Hide the border messages before and after the compact level up messages.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hideBorder = true;

        @Expose
        @ConfigOption(name = "Outside Bingo", desc = "Compact the level up chat messages outside of an bingo profile as well.")
        @ConfigEditorBoolean
        public boolean outsideBingo = false;
    }

    @Expose
    @ConfigOption(name = "Minion Craft Helper", desc = "Show how many more items you need to upgrade the minion in your inventory. Especially useful for bingo.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean minionCraftHelperEnabled = true;

    @Expose
    public Position minionCraftHelperPos = new Position(10, 10, false, true);
}
