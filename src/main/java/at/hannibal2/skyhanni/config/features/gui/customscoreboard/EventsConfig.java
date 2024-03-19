package at.hannibal2.skyhanni.config.features.gui.customscoreboard;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class EventsConfig {

    @Expose
    @ConfigOption(name = "Show all active events", desc = "Show all active events in the scoreboard instead of one.")
    @ConfigEditorBoolean
    public boolean showAllActiveEvents = false;

}
