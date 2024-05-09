package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.features.mining.MineshaftPityDisplay.MineshaftPityLines;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MineshaftPityDisplay {
    @Expose
    @ConfigOption(name = "Enable Display", desc = "Enable the Mineshaft Pity Display.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Stats List", desc = "Drag text to change the appearance of the display.")
    @ConfigEditorDraggableList
    public List<MineshaftPityLines> mineshaftPityLines = new ArrayList<>(Arrays.asList(
        MineshaftPityLines.TITLE,
        MineshaftPityLines.COUNTER,
        MineshaftPityLines.CHANCE,
        MineshaftPityLines.NEEDED_TO_PITY,
        MineshaftPityLines.TIME_SINCE_MINESHAFT
    ));

    @Expose
    @ConfigOption(name = "Modify Spawn Message", desc = "Modify the Mineshaft spawn message with more stats.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean modifyChatMessage = true;

    @Expose
    @ConfigLink(owner = MineshaftPityDisplay.class, field = "enabled")
    public Position position = new Position(16, 192, false, true);
}
