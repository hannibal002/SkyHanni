package at.hannibal2.skyhanni.config.features.inventory.experimentationtable;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.api.ExperimentationTableApi.ExperimentationMessages;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.List;

public class ExperimentsProfitTrackerConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Tracker for drops/XP you get from experiments.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Hide Messages", desc = "Change the messages to be hidden after completing Add-on/Main experiments.")
    @ConfigEditorDraggableList
    public List<ExperimentationMessages> hideMessages = new ArrayList<>();

    @Expose
    @ConfigOption(name = "Track Time Spent", desc = "Track time spent doing addons and experiments.")
    @ConfigEditorBoolean
    public boolean trackTimeSpent = false;

    @Expose
    @ConfigOption(name = "Track Used Bottles", desc = "Track thrown XP bottles while near the experimentation table.")
    @ConfigEditorBoolean
    public boolean trackUsedBottles = true;

    @Expose
    @ConfigOption(name = "Bottle Warnings", desc = "Display warnings once per session about bottles being auto-tracked.")
    @ConfigEditorBoolean
    public boolean bottleWarnings = true;

    @Expose
    @ConfigLink(owner = ExperimentsProfitTrackerConfig.class, field = "enabled")
    public Position position = new Position(20, 20);
}
