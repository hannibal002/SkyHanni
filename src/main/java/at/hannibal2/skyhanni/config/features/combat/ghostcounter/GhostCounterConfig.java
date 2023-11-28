package at.hannibal2.skyhanni.config.features.combat.ghostcounter;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.features.combat.ghostcounter.textformatting.TextFormattingConfig;
import at.hannibal2.skyhanni.features.combat.ghostcounter.GhostUtil;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorButton;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static at.hannibal2.skyhanni.config.features.combat.ghostcounter.GhostDisplayEntry.AVG_MAGIC_FIND;
import static at.hannibal2.skyhanni.config.features.combat.ghostcounter.GhostDisplayEntry.GHOSTS_KILLED;
import static at.hannibal2.skyhanni.config.features.combat.ghostcounter.GhostDisplayEntry.GHOST_PER_SORROW;
import static at.hannibal2.skyhanni.config.features.combat.ghostcounter.GhostDisplayEntry.GHOST_SINCE_SORROW;
import static at.hannibal2.skyhanni.config.features.combat.ghostcounter.GhostDisplayEntry.HIGHEST_KILL_COMBO;
import static at.hannibal2.skyhanni.config.features.combat.ghostcounter.GhostDisplayEntry.KILL_COMBO;
import static at.hannibal2.skyhanni.config.features.combat.ghostcounter.GhostDisplayEntry.SCAVENGER_COINS;
import static at.hannibal2.skyhanni.config.features.combat.ghostcounter.GhostDisplayEntry.SORROW;
import static at.hannibal2.skyhanni.config.features.combat.ghostcounter.GhostDisplayEntry.TITLE;

public class GhostCounterConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable the ghost counter (invisible creepers in the Dwarven Mines The Mist area).")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(
        name = "Display Text",
        desc = "Drag text to change the appearance of the overlay."
    )
    @ConfigEditorDraggableList()
    public List<GhostDisplayEntry> ghostDisplayText = new ArrayList<>(Arrays.asList(
        TITLE,
        GHOSTS_KILLED,
        SORROW,
        GHOST_SINCE_SORROW,
        GHOST_PER_SORROW,
        AVG_MAGIC_FIND,
        SCAVENGER_COINS,
        KILL_COMBO,
        HIGHEST_KILL_COMBO
    ));

    @ConfigOption(name = "Text Formatting", desc = "")
    @Accordion
    @Expose
    public TextFormattingConfig textFormatting = new TextFormattingConfig();

    @Expose
    @ConfigOption(name = "Extra space", desc = "Space between each line of text.")
    @ConfigEditorSlider(
        minValue = -5,
        maxValue = 10,
        minStep = 1)
    public int extraSpace = 1;

    @Expose
    @ConfigOption(name = "Pause Timer", desc = "How many seconds does it wait before pausing.")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 20,
        minStep = 1
    )
    public int pauseTimer = 3;

    @Expose
    @ConfigOption(name = "Show only in The Mist", desc = "Show the overlay only when you are in The Mist.")
    @ConfigEditorBoolean
    public boolean onlyOnMist = true;

    @Expose
    @ConfigOption(name = "Maxed Bestiary", desc = "Show progress to max bestiary instead of next level.")
    @ConfigEditorBoolean
    public boolean showMax = false;

    @ConfigOption(name = "Reset", desc = "Reset the counter.")
    @ConfigEditorButton(buttonText = "Reset")
    public Runnable resetCounter = GhostUtil.INSTANCE::reset;

    @Expose
    public Position position = new Position(50, 50, false, true);
}
