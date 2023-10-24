package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MiningConfig {

    @Expose
    @ConfigOption(name = "Powder Tracker", desc = "")
    @Accordion
    public PowderTrackerConfig powderTracker = new PowderTrackerConfig();

    public static class PowderTrackerConfig {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Enable the Powder Tracker overlay for mining.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Only when Grinding", desc = "Only show the overlay when powder grinding.")
        @ConfigEditorBoolean
        public boolean onlyWhenPowderGrinding = false;

        @Expose
        @ConfigOption(name = "Great Explorer", desc = "Enable this if your Great Explorer perk is maxed.")
        @ConfigEditorBoolean
        public boolean greatExplorerMaxed = false;

        @Expose
        @ConfigOption(
                name = "Text Format",
                desc = "Drag text to change the appearance of the overlay."
        )
        @ConfigEditorDraggableList(
                exampleText = {
                        "§b§lPowder Tracker",
                        "§7Display Mode: §a[Total] §e[This Session]",
                        "§d852 Total chests Picked §7(950/h)",
                        "§bx2 Powder: §aActive!",
                        "§b250,420 §aMithril Powder §7(350,000/h)",
                        "§b250,420 §dGemstone Powder §7(350,000/h)",
                        "",
                        "§b129 §bDiamond Essence §7(600/h)",
                        "§b234 §6Gold Essence §7(700/h)",
                        "",
                        "§50§7-§90§7-§a0§f-0 §cRuby Gemstone",
                        "§50§7-§90§7-§a0§f-0 §bSapphire Gemstone",
                        "§50§7-§90§7-§a0§f-0 §6Amber Gemstone",
                        "§50§7-§90§7-§a0§f-0 §5Amethyst Gemstone",
                        "§50§7-§90§7-§a0§f-0 §aJade Gemstone",
                        "§50§7-§90§7-§a0§f-0 §eTopaz Gemstone",

                        "§b14 §9FTX 3070",
                        "§b14 §9Electron Transmitter",
                        "§b14 §9Robotron Reflector",
                        "§b14 §9Superlite Motor",
                        "§b14 §9Control Switch",
                        "§b14 §9Synthetic Heart",
                        "§b14 §9Total Robot Parts",

                        "§90§7-§a0§7-§c0§f-§e0§f-§30 §fGoblin Egg",

                        "§b12 §aWishing Compass",

                        "§b320 §aSludge Juice",
                        "§b2 §9Ascension Rope",
                        "§b6 §5Treasurite",
                        "§b4 §6Jungle Heart",
                        "§b1 §5Pickonimbus 2000",
                        "§b14 §aYoggie",
                        "§b9 §fPrehistoric Egg",
                        "§b25 §aOil Barrel"
                }
        )
        public Property<List<Integer>> textFormat = Property.of(new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18)));

        @Expose
        public Position position = new Position(-274, 0, false, true);

    }

    @Expose
    @ConfigOption(name = "Highlight Commission Mobs", desc = "Highlight Mobs that are part of active commissions.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightCommissionMobs = false;

    @Expose
    @ConfigOption(name = "King Talisman Helper", desc = "Show kings you have not talked to yet, and when the next missing king will appear.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean kingTalismanHelper = false;

    @Expose
    public Position kingTalismanHelperPos = new Position(-400, 220, false, true);

    @Expose
    @ConfigOption(name = "Names in Core", desc = "Show the names of the 4 areas while in the center of the Crystal Hollows.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean crystalHollowsNamesInCore = false;
}
