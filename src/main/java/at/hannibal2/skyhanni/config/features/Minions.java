package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import io.github.moulberry.moulconfig.annotations.*;

public class Minions {

    @ConfigOption(name = "Name Display", desc = "Show the minion name and tier over the minion.")
    @ConfigEditorBoolean
    public boolean nameDisplay = true;

    @ConfigOption(name = "Only Tier", desc = "Show only the tier number over the minion. (Useful for bingo)")
    @ConfigEditorBoolean
    public boolean nameOnlyTier = false;

    @ConfigOption(name = "Last Clicked", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean lastClickedMinion = false;

    @ConfigOption(name = "Last Minion Display", desc = "Marks the location of the last clicked minion, even through walls.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean lastClickedMinionDisplay = false;

    @ConfigOption(
            name = "Last Minion Color",
            desc = "The color in which the last minion should be displayed."
    )
    @ConfigEditorColour
    @ConfigAccordionId(id = 0)
    public String lastOpenedMinionColor = "0:245:85:255:85";

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

    @ConfigOption(name = "Emptied Time Display", desc = "Show the time when the hopper in the minion was last emptied.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean emptiedTimeDisplay = false;

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

    @ConfigOption(name = "Hopper Profit Display", desc = "Use the hopper's held coins and the last empty time to calculate the coins per day.")
    @ConfigEditorBoolean
    public boolean hopperProfitDisplay = true;

    public Position hopperProfitPos = new Position(10, 10, false, true);

    @ConfigOption(name = "Hide Mob Nametag", desc = "Hiding the nametag of mobs close to minions.")
    @ConfigEditorBoolean
    public boolean hideMobsNametagNearby = false;
}
