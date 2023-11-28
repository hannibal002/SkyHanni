package at.hannibal2.skyhanni.config.features.combat.ghostcounter;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.HasLegacyId;
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

import static at.hannibal2.skyhanni.config.features.combat.ghostcounter.GhostCounterConfig.GhostDisplayEntry.AVG_MAGIC_FIND;
import static at.hannibal2.skyhanni.config.features.combat.ghostcounter.GhostCounterConfig.GhostDisplayEntry.GHOSTS_KILLED;
import static at.hannibal2.skyhanni.config.features.combat.ghostcounter.GhostCounterConfig.GhostDisplayEntry.GHOST_PER_SORROW;
import static at.hannibal2.skyhanni.config.features.combat.ghostcounter.GhostCounterConfig.GhostDisplayEntry.GHOST_SINCE_SORROW;
import static at.hannibal2.skyhanni.config.features.combat.ghostcounter.GhostCounterConfig.GhostDisplayEntry.HIGHEST_KILL_COMBO;
import static at.hannibal2.skyhanni.config.features.combat.ghostcounter.GhostCounterConfig.GhostDisplayEntry.KILL_COMBO;
import static at.hannibal2.skyhanni.config.features.combat.ghostcounter.GhostCounterConfig.GhostDisplayEntry.SCAVENGER_COINS;
import static at.hannibal2.skyhanni.config.features.combat.ghostcounter.GhostCounterConfig.GhostDisplayEntry.SORROW;
import static at.hannibal2.skyhanni.config.features.combat.ghostcounter.GhostCounterConfig.GhostDisplayEntry.TITLE;

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

    public enum GhostDisplayEntry implements HasLegacyId {
        TITLE("§6Ghosts Counter", 0),
        GHOSTS_KILLED("  §bGhost Killed: 42", 1),
        SORROW("  §bSorrow: 6", 2),
        GHOST_SINCE_SORROW("  §bGhost since Sorrow: 1", 3),
        GHOST_PER_SORROW("  §bGhosts/Sorrow: 5", 4),
        VOLTA("  §bVolta: 6", 5),
        PLASMA("  §bPlasma: 8", 6),
        GHOSTLY_BOOTS("  §bGhostly Boots: 1", 7),
        BAG_OF_CASH("  §bBag Of Cash: 4", 8),
        AVG_MAGIC_FIND("  §bAvg Magic Find: 271", 9),
        SCAVENGER_COINS("  §bScavenger Coins: 15,000", 10),
        KILL_COMBO("  §bKill Combo: 14", 11),
        HIGHEST_KILL_COMBO("  §bHighest Kill Combo: 96", 12),
        SKILL_XP_GAINED("  §bSkill XP Gained: 145,648", 13),
        BESTIARY("  §bBestiary 1: 0/10", 14),
        XP_PER_HOUR("  §bXP/h: 810,410", 15),
        KILLS_PER_HOUR("  §bKills/h: 420", 16),
        ETA("  §bETA: 14d", 17),
        MONEY_PER_HOUR("  §bMoney/h: 13,420,069", 18),
        MONEY_MADE("  §bMoney made: 14B", 19),
        ;

        private final String str;
        private final int legacyId;

        GhostDisplayEntry(String str, int legacyId) {
            this.str = str;
            this.legacyId = legacyId;
        }

        // Constructor if new enum elements are added post-migration
        GhostDisplayEntry(String str) {
            this(str, -1);
        }

        @Override
        public int getLegacyId() {
            return legacyId;
        }

        @Override
        public String getStr() {
            return str;
        }
    }

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
