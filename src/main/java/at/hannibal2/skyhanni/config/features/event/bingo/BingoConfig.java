package at.hannibal2.skyhanni.config.features.event.bingo;

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
    public BingoCardConfig bingoCard = new BingoCardConfig();

    @Expose
    @ConfigOption(name = "Compact Chat Messages", desc = "")
    @Accordion
    public CompactChatConfig compactChat = new CompactChatConfig();

    @Expose
    @ConfigOption(name = "Minion Craft Helper", desc = "Show how many more items you need to upgrade the minion in your inventory. Especially useful for Bingo.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean minionCraftHelperEnabled = true;

    @Expose
    public Position minionCraftHelperPos = new Position(10, 10, false, true);
}
