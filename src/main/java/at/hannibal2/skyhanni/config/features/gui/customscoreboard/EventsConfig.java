package at.hannibal2.skyhanni.config.features.gui.customscoreboard;

import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardEventEntry;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;

import java.util.ArrayList;
import java.util.List;

public class EventsConfig {

    @Expose
    @ConfigOption(
        name = "Events Priority",
        desc = "Drag your list to select the priority of each event."
    )
    @ConfigEditorDraggableList()
    public Property<List<ScoreboardEventEntry>> eventEntries = Property.of(new ArrayList<>(ScoreboardEventEntry.defaultOption));

    @ConfigOption(name = "Reset Events Priority", desc = "Reset the priority of all events.")
    @ConfigEditorButton(buttonText = "Reset")
    public Runnable reset = () -> {
    };

    @Expose
    @ConfigOption(name = "Show all active events", desc = "Show all active events in the scoreboard instead of the one with the highest priority.")
    @ConfigEditorBoolean
    public boolean showAllActiveEvents = false;

}
