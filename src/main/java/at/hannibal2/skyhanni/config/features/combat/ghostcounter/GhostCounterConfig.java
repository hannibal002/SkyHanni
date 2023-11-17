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
    @ConfigEditorDraggableList(
        exampleText = {
            "§6Ghosts Counter",
            "  §bGhost Killed: 42",
            "  §bSorrow: 6",
            "  §bGhost since Sorrow: 1",
            "  §bGhosts/Sorrow: 5",
            "  §bVolta: 6",
            "  §bPlasma: 8",
            "  §bGhostly Boots: 1",
            "  §bBag Of Cash: 4",
            "  §bAvg Magic Find: 271",
            "  §bScavenger Coins: 15,000",
            "  §bKill Combo: 14",
            "  §bHighest Kill Combo: 96",
            "  §bSkill XP Gained: 145,648",
            "  §bBestiary 1: 0/10",
            "  §bXP/h: 810,410",
            "  §bKills/h: 420",
            "  §bETA: 14d",
            "  §bMoney/h: 13,420,069",
            "  §bMoney made: 14B"
        }
    )
    public List<Integer> ghostDisplayText = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 9, 10, 11, 12));

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
