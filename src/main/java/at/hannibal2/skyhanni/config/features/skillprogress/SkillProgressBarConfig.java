package at.hannibal2.skyhanni.config.features.skillprogress;

import at.hannibal2.skyhanni.SkyHanniMod;
import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;

public class SkillProgressBarConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable or disable the progress bar.")
    @ConfigEditorBoolean
    @FeatureToggle
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
    @ConfigOption(name = "Bar Color", desc = "Color of the progress bar.\n§eIgnored if Chroma is enabled.")
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

    @Expose
    @ConfigOption(name = "Color Per Skill", desc = "Change the bar color depending of the skill.")
    @ConfigEditorBoolean
    public boolean colorPerSkill = false;

    @Expose
    @ConfigOption(name = "Combat", desc = "Color for the combat bar.")
    @ConfigEditorColour
    public Property<String> combatBarColor = Property.of("0:245:255:0:59");

    @Expose
    @ConfigOption(name = "Farming", desc = "Color for the farming bar.")
    @ConfigEditorColour
    public Property<String> farmingBarColor = Property.of("0:245:85:255:85");

    @Expose
    @ConfigOption(name = "Fishing", desc = "Color for the fishing bar.")
    @ConfigEditorColour
    public Property<String> fishingBarColor = Property.of("0:245:0:64:255");

    @Expose
    @ConfigOption(name = "Mining", desc = "Color for the mining bar.")
    @ConfigEditorColour
    public Property<String> miningBarColor = Property.of("0:245:0:255:249");

    @Expose
    @ConfigOption(name = "Foraging", desc = "Color for the foraging bar.")
    @ConfigEditorColour
    public Property<String> foragingBarColor = Property.of("0:245:30:126:0");

    @Expose
    @ConfigOption(name = "Enchanting", desc = "Color for the enchanting bar.")
    @ConfigEditorColour
    public Property<String> enchantingBarColor = Property.of("0:255:255:248:0");

    @Expose
    @ConfigOption(name = "Alchemy", desc = "Color for the alchemy bar.")
    @ConfigEditorColour
    public Property<String> alchemyBarColor = Property.of("0:245:255:141:206");

    @Expose
    @ConfigOption(name = "Carpentry", desc = "Color for the carpentry bar.")
    @ConfigEditorColour
    public Property<String> carpentryBarColor = Property.of("0:245:255:250:242");

    @Expose
    @ConfigOption(name = "Taming", desc = "Color for the taming bar.")
    @ConfigEditorColour
    public Property<String> tamingBarColor = Property.of("0:245:255:0:171");
}
