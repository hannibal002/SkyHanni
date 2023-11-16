package at.hannibal2.skyhanni.config.features.chat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class ChatSymbols {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Adds extra symbols to the chat such as those from ironman, " +
        "stranded, bingo or nether factions and places them next to your regular player emblems. " +
        "§cDoes not work with hide rank hider!")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Chat Symbol Location", desc = "Determines where the symbols should go in chat in relation to the " +
        "player's name. Hidden will hide all emblems from the chat. §eRequires above setting to be on to hide the symbols.")
    @ConfigEditorDropdown(values = {"Left", "Right", "Hidden"})
    public int symbolLocation = 0;
}
