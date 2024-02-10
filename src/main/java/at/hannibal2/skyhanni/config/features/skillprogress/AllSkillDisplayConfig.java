package at.hannibal2.skyhanni.config.features.skillprogress;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static at.hannibal2.skyhanni.config.features.skillprogress.AllSkillDisplayConfig.SkillEntry.ALCHEMY;
import static at.hannibal2.skyhanni.config.features.skillprogress.AllSkillDisplayConfig.SkillEntry.CARPENTRY;
import static at.hannibal2.skyhanni.config.features.skillprogress.AllSkillDisplayConfig.SkillEntry.COMBAT;
import static at.hannibal2.skyhanni.config.features.skillprogress.AllSkillDisplayConfig.SkillEntry.ENCHANTING;
import static at.hannibal2.skyhanni.config.features.skillprogress.AllSkillDisplayConfig.SkillEntry.FARMING;
import static at.hannibal2.skyhanni.config.features.skillprogress.AllSkillDisplayConfig.SkillEntry.FISHING;
import static at.hannibal2.skyhanni.config.features.skillprogress.AllSkillDisplayConfig.SkillEntry.FORAGING;
import static at.hannibal2.skyhanni.config.features.skillprogress.AllSkillDisplayConfig.SkillEntry.MINING;
import static at.hannibal2.skyhanni.config.features.skillprogress.AllSkillDisplayConfig.SkillEntry.TAMING;

public class AllSkillDisplayConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show a display with all skills progress.")
    @ConfigEditorBoolean
    public Property<Boolean> enabled = Property.of(false);

    @Expose
    @ConfigOption(name = "Text", desc = "Choose what skills you want to see in the display.")
    @ConfigEditorDraggableList
    public List<SkillEntry> skillEntryList = new ArrayList<>(Arrays.asList(
        ALCHEMY,
        CARPENTRY,
        COMBAT,
        ENCHANTING,
        FARMING,
        FISHING,
        FORAGING,
        MINING,
        TAMING
    ));

    public enum SkillEntry {
        ALCHEMY("§bAlchemy"),
        CARPENTRY("§bCarpentry"),
        COMBAT("§bCombat"),
        ENCHANTING("§bEnchanting"),
        FARMING("§bFarming"),
        FISHING("§bFishing"),
        FORAGING("§bForaging"),
        MINING("§bMining"),
        TAMING("§bTaming"),

        ;

        private final String str;

        SkillEntry(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
