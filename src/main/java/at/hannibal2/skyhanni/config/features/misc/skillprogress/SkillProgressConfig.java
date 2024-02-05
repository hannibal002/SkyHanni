package at.hannibal2.skyhanni.config.features.misc.skillprogress;

import at.hannibal2.skyhanni.SkyHanniMod;
import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static at.hannibal2.skyhanni.config.features.misc.skillprogress.SkillProgressConfig.AllSkillEntry.ALCHEMY;
import static at.hannibal2.skyhanni.config.features.misc.skillprogress.SkillProgressConfig.AllSkillEntry.CARPENTRY;
import static at.hannibal2.skyhanni.config.features.misc.skillprogress.SkillProgressConfig.AllSkillEntry.COMBAT;
import static at.hannibal2.skyhanni.config.features.misc.skillprogress.SkillProgressConfig.AllSkillEntry.ENCHANTING;
import static at.hannibal2.skyhanni.config.features.misc.skillprogress.SkillProgressConfig.AllSkillEntry.FARMING;
import static at.hannibal2.skyhanni.config.features.misc.skillprogress.SkillProgressConfig.AllSkillEntry.FISHING;
import static at.hannibal2.skyhanni.config.features.misc.skillprogress.SkillProgressConfig.AllSkillEntry.FORAGING;
import static at.hannibal2.skyhanni.config.features.misc.skillprogress.SkillProgressConfig.AllSkillEntry.MINING;
import static at.hannibal2.skyhanni.config.features.misc.skillprogress.SkillProgressConfig.AllSkillEntry.TAMING;

public class SkillProgressConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the Skill Progress Display.")
    @ConfigEditorBoolean
    @FeatureToggle
    public Property<Boolean> enabled = Property.of(false);

    @Expose
    @ConfigOption(name = "Progress Bar Config", desc = "")
    @Accordion
    public ProgressBarConfig progressBarConfig = new ProgressBarConfig();

    public static class ProgressBarConfig {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Enable/Disable the progress bar.")
        @ConfigEditorBoolean
        public Property<Boolean> enabled = Property.of(false);

        @Expose
        @ConfigOption(name = "Textured Bar", desc = "Use a textured progress bar.\n§eCan be changed with a resource pack.")
        @ConfigEditorBoolean
        public Property<Boolean> useTexturedBar = Property.of(false);

        @Expose
        @ConfigOption(name = "Chroma", desc = "Use the SBA like chroma effect on the bar.\n§eIf enabled, ignore the Bar Color setting.")
        @ConfigEditorBoolean
        public Property<Boolean> useChroma = Property.of(false);

        @Expose
        @ConfigOption(name = "Bar Color", desc = "Color of the progress bar.\n§eIgnored if Chroma is enabled")
        @ConfigEditorColour
        public String barStartColor = "0:255:255:0:0";

        @Expose
        @ConfigOption(name = "Textured Bar", desc = "")
        @Accordion
        public TexturedBar texturedBar = new TexturedBar();

        public static class TexturedBar {

            @Expose
            @ConfigOption(name = "Used Texture", desc = "Choose what texture to use.")
            @ConfigEditorDropdown
            public Property<UsedTexture> usedTexture = Property.of(UsedTexture.MATCH_PACK);

            public enum UsedTexture {
                MATCH_PACK("Match Resource Pack", "minecraft:textures/gui/icons.png"),
                CUSTOM_1("Texture 1", SkyHanniMod.MODID + ":bars/1.png"),
                CUSTOM_2("Texture 2", SkyHanniMod.MODID + ":bars/2.png"),
                CUSTOM_3("Texture 3", SkyHanniMod.MODID + ":bars/3.png"),
                CUSTOM_4("Texture 4", SkyHanniMod.MODID + ":bars/4.png"),
                CUSTOM_5("Texture 5", SkyHanniMod.MODID + ":bars/5.png"),
                ;

                private final String str;
                private final String path;

                UsedTexture(String str, String path) {
                    this.str = str;
                    this.path = path;
                }

                public String getPath() {
                    return path;
                }

                @Override
                public String toString() {
                    return str;
                }
            }

            @Expose
            @ConfigOption(name = "Width", desc = "Modify the width of the bar.\n" +
                "§eDefault: 182\n" +
                "§c!!Do not work for now!!")
            @ConfigEditorSlider(minStep = 1, minValue = 16, maxValue = 1024)
            public int width = 182;

            @Expose
            @ConfigOption(name = "Height", desc = "Modify the height of the bar.\n" +
                "§eDefault: 5\n" +
                "§c!!Do not work for now!!")
            @ConfigEditorSlider(minStep = 1, minValue = 3, maxValue = 16)
            public int height = 5;
        }

        @Expose
        @ConfigOption(name = "Regular Bar", desc = "")
        @Accordion
        public RegularBar regularBar = new RegularBar();

        public static class RegularBar {
            @Expose
            @ConfigOption(name = "Width", desc = "Modify the width of the bar.")
            @ConfigEditorSlider(minStep = 1, minValue = 100, maxValue = 1000)
            public int width = 182;

            @Expose
            @ConfigOption(name = "Height", desc = "Modify the height of the bar.")
            @ConfigEditorSlider(minStep = 1, minValue = 3, maxValue = 15)
            public int height = 6;
        }
    }

    @Expose
    @ConfigOption(name = "Always Show", desc = "Always show the skill progress.")
    @ConfigEditorBoolean
    public Property<Boolean> alwaysShow = Property.of(false);

    @Expose
    @ConfigOption(name = "Show action left", desc = "Show action left until you reach the next level.")
    @ConfigEditorBoolean
    public Property<Boolean> showActionLeft = Property.of(false);

    @Expose
    @ConfigOption(name = "Use percentage", desc = "Use percentage instead of XP.")
    @ConfigEditorBoolean
    public Property<Boolean> usePercentage = Property.of(false);

    @Expose
    @ConfigOption(name = "Use Icon", desc = "Show the skill icon in the display.")
    @ConfigEditorBoolean
    public Property<Boolean> useIcon = Property.of(true);

    @Expose
    @ConfigOption(name = "Use Skill Name", desc = "Show the skill name in the display.")
    @ConfigEditorBoolean
    public Property<Boolean> useSkillName = Property.of(false);

    @Expose
    @ConfigOption(name = "Show Level", desc = "Show your current level in the display.")
    @ConfigEditorBoolean
    public Property<Boolean> showLevel = Property.of(true);

    @Expose
    @ConfigOption(name = "Overflow Config", desc = "")
    @Accordion
    public SkillOverflowConfig overflowConfig = new SkillOverflowConfig();


    @Expose
    @ConfigOption(name = "All Skills Display", desc = "Show a display with all skills progress.")
    @ConfigEditorBoolean
    public Property<Boolean> showAllSkillProgress = Property.of(false);

    @Expose
    @ConfigOption(name = "All Skill Text", desc = "Choose skills you want to see in the display.")
    @ConfigEditorDraggableList
    public List<AllSkillEntry> allskillEntryList = new ArrayList<>(Arrays.asList(
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

    public enum AllSkillEntry {
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

        AllSkillEntry(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @ConfigOption(name = "Skill ETA Display", desc = "Show a display of your current active skill\n" +
        "with the XP/hour rate and ETA to the next level.")
    @ConfigEditorBoolean
    public Property<Boolean> showEtaSkillProgress = Property.of(false);

    @Expose
    public Position position = new Position(348, -105, false, true);

    @Expose
    public Position barPosition = new Position(363, -87, false, true);

    @Expose
    public Position allSkillPosition = new Position(5, 209, false, true);

    @Expose
    public Position etaPosition = new Position(5, 155, false, true);
}
