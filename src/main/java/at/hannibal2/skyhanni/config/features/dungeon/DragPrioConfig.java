package at.hannibal2.skyhanni.config.features.dungeon;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class DragPrioConfig {
    @Expose
    @ConfigOption(name = "Set Power", desc = "Set the power that you split on.")
    @ConfigEditorSlider(
            minValue = 0,
            maxValue = 32,
            minStep = 1
    )
    public int splitPower = 22;

    @Expose
    @ConfigOption(name = "Easy Power", desc = "Set the power that you split on for easy drags (O/P/G).")
    @ConfigEditorSlider(
            minValue = 0,
            maxValue = 32,
            minStep = 1
    )
    public int easyPower = 19;

    @Expose
    @ConfigOption(name = "Show Non-Split drags", desc = "Display \"X Dragon is spawning!\" on non-split drags.")
    @ConfigEditorBoolean
    public boolean showSingleDragons = true;

    @Expose
    @ConfigOption(name = "Send Split Message", desc = "Send the \"Bers Team: Arch Team:\" message.")
    @ConfigEditorBoolean
    public boolean sendMessage = false;


    @Expose
    @ConfigOption(name = "Say Split", desc = "Send \"Split\" when splitting.")
    @ConfigEditorBoolean
    public boolean saySplit = true;
}
