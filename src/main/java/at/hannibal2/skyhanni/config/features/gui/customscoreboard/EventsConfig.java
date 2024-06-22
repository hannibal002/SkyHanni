package at.hannibal2.skyhanni.config.features.gui.customscoreboard;

import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardEventManager;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.List;

public class EventsConfig {

    @Expose
    @ConfigOption(
        name = "Events Priority",
        desc = "Drag your list to select the priority of each event."
    )
    @ConfigEditorDraggableList()
    public List<ScoreboardEventManager> eventEntries = new ArrayList<>(ScoreboardEventManager.defaultOption);

    @Expose
    @ConfigOption(name = "Show all active events", desc = "Show all active events in the scoreboard instead of the one with the highest priority.")
    @ConfigEditorBoolean
    public boolean showAllActiveEvents = false;

}
