package at.hannibal2.skyhanni.config.features.minion;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class MinionsConfig {

    @Expose
    @ConfigOption(name = "Name Display", desc = "Show the minion name and tier over the minion.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean nameDisplay = true;

    @Expose
    @ConfigOption(name = "Only Tier", desc = "Show only the tier number over the minion. (Useful for Bingo)")
    @ConfigEditorBoolean
    public boolean nameOnlyTier = false;

    @Expose
    @ConfigOption(name = "Last Clicked", desc = "")
    @Accordion
    public LastClickedMinionConfig lastClickedMinion = new LastClickedMinionConfig();

    @Expose
    @ConfigOption(name = "Emptied Time", desc = "")
    @Accordion
    public EmptiedTimeConfig emptiedTime = new EmptiedTimeConfig();

    @Expose
    @ConfigOption(name = "Hopper Profit Display", desc = "Use the hopper's held coins and the last empty time to calculate the coins per day.")
    @ConfigEditorBoolean
    public boolean hopperProfitDisplay = true;

    @Expose
    public Position hopperProfitPos = new Position(360, 90, false, true);

    @Expose
    @ConfigOption(name = "Hide Mob Nametag", desc = "Hiding the nametag of mobs close to minions.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideMobsNametagNearby = false;
}
