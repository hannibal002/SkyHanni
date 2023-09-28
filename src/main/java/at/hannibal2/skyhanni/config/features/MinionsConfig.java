package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigAccordionId;
import io.github.moulberry.moulconfig.annotations.ConfigEditorAccordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
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

    @ConfigOption(name = "Last Clicked", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean lastClickedMinion = false;

    @Expose
    @ConfigOption(name = "Last Minion Display", desc = "Marks the location of the last clicked minion, even through walls.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    @FeatureToggle
    public boolean lastClickedMinionDisplay = false;

    @Expose
    @ConfigOption(
            name = "Last Minion Color",
            desc = "The color in which the last minion should be displayed."
    )
    @ConfigEditorColour
    @ConfigAccordionId(id = 0)
    public String lastOpenedMinionColor = "0:245:85:255:85";

    @Expose
    @ConfigOption(
            name = "Last Minion Time",
            desc = "Time in seconds how long the last minion should be displayed."
    )
    @ConfigEditorSlider(
            minValue = 3,
            maxValue = 120,
            minStep = 1
    )
    @ConfigAccordionId(id = 0)
    public int lastOpenedMinionTime = 20;

    @ConfigOption(name = "Emptied Time", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean emptiedTime = false;

    @Expose
    @ConfigOption(name = "Emptied Time Display", desc = "Show the time when the hopper in the minion was last emptied.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    @FeatureToggle
    public boolean emptiedTimeDisplay = false;

    @Expose
    @ConfigOption(
            name = "Distance",
            desc = "Maximum distance to display minion data."
    )
    @ConfigEditorSlider(
            minValue = 3,
            maxValue = 30,
            minStep = 1
    )
    @ConfigAccordionId(id = 1)
    public int distance = 10;

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
