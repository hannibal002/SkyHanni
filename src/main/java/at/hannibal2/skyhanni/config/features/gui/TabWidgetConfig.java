package at.hannibal2.skyhanni.config.features.gui;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.features.gui.TabWidgetDisplay;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TabWidgetConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Enables the gui elements for the selected widgets.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Widgets", desc = "")
    @ConfigEditorDraggableList
    public List<TabWidgetDisplay> display = new ArrayList<>();

    @Expose
    public List<Position> displayPositions = Stream.generate(Position::new)
        .limit(TabWidgetDisplay.getEntries().size())
        .collect(Collectors.toCollection(ArrayList::new));
}
