package at.hannibal2.skyhanni.config.features.gui.customscoreboard;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.features.misc.customscoreboard.ScoreboardElements;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomScoreboardConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show a custom scoreboard instead of the vanilla one."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(
        name = "Appearance",
        desc = "Drag text to change the appearance of the advanced scoreboard." // supporting both custom & advanced search
    )
    @ConfigEditorDraggableList()
    public List<ScoreboardElements> scoreboardEntries = new ArrayList<>(Arrays.asList(ScoreboardElements.values()));

    @Expose
    @ConfigOption(name = "Display Options", desc = "")
    @Accordion
    public DisplayConfig displayConfig = new DisplayConfig();

    @Expose
    @ConfigOption(name = "Information Filtering", desc = "")
    @Accordion
    public InformationFilteringConfig informationFilteringConfig = new InformationFilteringConfig();

    @Expose
    @ConfigOption(name = "Background Options", desc = "")
    @Accordion
    public BackgroundConfig backgroundConfig = new BackgroundConfig();

    @Expose
    @ConfigOption(name = "Party Options", desc = "")
    @Accordion
    public PartyConfig partyConfig = new PartyConfig();

    @Expose
    @ConfigOption(name = "Mayor Options", desc = "")
    @Accordion
    public MayorConfig mayorConfig = new MayorConfig();

    @Expose
    @ConfigOption(name = "Unknown Lines warning", desc = "Gives a chat warning when unknown lines are found in the scoreboard.")
    @ConfigEditorBoolean
    public boolean unknownLinesWarning = true;

    @Expose
    public Position position = new Position(10, 80, false, true);
}
