package at.hannibal2.skyhanni.config.features.event.bingo;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class BingoCardConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Displays the Bingo Card.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;
    @Expose
    @ConfigOption(name = "Quick Toggle", desc = "Quickly toggle the Bingo Card or the step helper by sneaking with SkyBlock Menu in hand.")
    @ConfigEditorBoolean
    public boolean quickToggle = true;

    @Expose
    @ConfigOption(name = "Bingo Steps", desc = "Show help with the next step in Bingo instead of the Bingo Card. " +
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
        desc = "Show tips and difficulty for bingo goals inside the Bingo Card inventory.\n" +
            "These tips are made from inspirations and guides from the community,\n" +
            "aiming to help you to complete the bingo card."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean bingoSplashGuide = true;

    @Expose
    public Position bingoCardPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Next Tip Duration", desc = "Show the duration until the next hidden personal goal gets a tip revealed.")
    @ConfigEditorBoolean
    public Property<Boolean> nextTipDuration = Property.of(true);

    @Expose
    @ConfigOption(name = "Hide Difficulty When Done", desc = "Remove the background difficulty color in the bingo card inventory when the goal is done.")
    @ConfigEditorBoolean
    public boolean hideDoneDifficulty = true;
}
