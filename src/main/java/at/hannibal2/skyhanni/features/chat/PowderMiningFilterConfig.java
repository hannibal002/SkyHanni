package at.hannibal2.skyhanni.features.chat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class PowderMiningFilterConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Hide messages while opening chests in the Crystal Hollows.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(
        name = "Powder", desc = "Hide §dGemstone §7and §aMithril §7Powder rewards under a certain amount." +
        "\n§a0§7: §aShow all\n§c20000§7: §cHide all"
    )
    @ConfigEditorSlider(minValue = 0, maxValue = 20000, minStep = 500)
    public int powderFilterThreshold = 1000;

    @Expose
    @ConfigOption(
        name = "Essence", desc = "Hide §6Gold §7and §bDiamond §7Essence rewards under a certain amount." +
        "\n§a0§7: §aShow all\n§c10§7: §cHide all"
    )
    @ConfigEditorSlider(minValue = 0, maxValue = 10, minStep = 1)
    public int essenceFilterThreshold = 3;

    @Expose
    @ConfigOption(name = "Gemstones", desc = "")
    @Accordion
    public PowderMiningGemstoneFilterConfig gemstoneFilterConfig = new PowderMiningGemstoneFilterConfig();

    @Expose
    @ConfigOption(name = "Ascension Rope", desc = "Hide Ascension Rope rewards.")
    @ConfigEditorBoolean
    public boolean ascensionRope = true;

    @Expose
    @ConfigOption(name = "Wishing Compass", desc = "Hide Wishing Compass rewards.")
    @ConfigEditorBoolean
    public boolean wishingCompass = true;

    @Expose
    @ConfigOption(name = "Oil Barrel", desc = "Hide Oil Barrel rewards.")
    @ConfigEditorBoolean
    public boolean oilBarrel = true;

    @Expose
    @ConfigOption(name = "Prehistoric Egg", desc = "Hide Prehistoric Egg rewards.")
    @ConfigEditorBoolean
    public boolean prehistoricEgg = false;

    @Expose
    @ConfigOption(name = "Pickonimbus", desc = "Hide §5Pickonimbus 2000 §7rewards.")
    @ConfigEditorBoolean
    public boolean pickonimbus = false;

    @Expose
    @ConfigOption(name = "Jungle Heart", desc = "Hide Jungle Heart rewards.")
    @ConfigEditorBoolean
    public boolean jungleHeart = true;

    @Expose
    @ConfigOption(name = "Sludge Juice", desc = "Hide Sludge Juice rewards.")
    @ConfigEditorBoolean
    public boolean sludgeJuice = true;

    @Expose
    @ConfigOption(name = "Yoggie", desc = "Hide Yoggie rewards.")
    @ConfigEditorBoolean
    public boolean yoggie = true;

    @Expose
    @ConfigOption(name = "Robot Parts", desc = "Hide all Robot Part rewards.")
    @ConfigEditorBoolean
    public boolean robotParts = false;

    @Expose
    @ConfigOption(name = "Treasurite", desc = "Hide treasurite rewards.")
    @ConfigEditorBoolean
    public boolean treasurite = true;

    @Expose
    @ConfigOption(name = "Goblin Egg", desc = "Hide Goblin Egg rewards that are below a certain rarity.")
    @ConfigEditorDropdown
    public GoblinEggFilterEntry goblinEggs = GoblinEggFilterEntry.YELLOW_UP;

    public enum GoblinEggFilterEntry {
        SHOW_ALL("Show all"),
        HIDE_ALL("Hide all"),
        GREEN_UP("Show §aGreen§7+"),
        YELLOW_UP("Show §eYellow§7+"),
        RED_UP("Show §cRed§&+"),
        BLUE_ONLY("Show §3Blue only");

        private final String str;

        GoblinEggFilterEntry(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

}
