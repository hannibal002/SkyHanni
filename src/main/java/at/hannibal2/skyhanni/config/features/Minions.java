package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigEditorBoolean;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigEditorColour;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigEditorSlider;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.ConfigOption;
import com.google.gson.annotations.Expose;

public class Minions {

    @Expose
    @ConfigOption(name = "Last Minion Display", desc = "Show the last opened minion on your island")
    @ConfigEditorBoolean
    public boolean lastOpenedMinionDisplay = false;

    @Expose
    @ConfigOption(
            name = "Last Minion Color",
            desc = "The colour in which the last minion should be displayed"
    )
    @ConfigEditorColour
    public String lastOpenedMinionColor = "0:245:85:255:85";

    @Expose
    @ConfigOption(
            name = "Last Minion Time",
            desc = "Time in seconds how long the last minion should be displayed"
    )
    @ConfigEditorSlider(
            minValue = 3,
            maxValue = 120,
            minStep = 1
    )
    public int lastOpenedMinionTime = 20;
}
