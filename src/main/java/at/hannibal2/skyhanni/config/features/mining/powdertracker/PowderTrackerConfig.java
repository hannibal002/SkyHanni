package at.hannibal2.skyhanni.config.features.mining.powdertracker;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static at.hannibal2.skyhanni.config.features.mining.powdertracker.PowderDisplayEntry.AMBER;
import static at.hannibal2.skyhanni.config.features.mining.powdertracker.PowderDisplayEntry.AMETHYST;
import static at.hannibal2.skyhanni.config.features.mining.powdertracker.PowderDisplayEntry.DIAMOND_ESSENCE;
import static at.hannibal2.skyhanni.config.features.mining.powdertracker.PowderDisplayEntry.DISPLAY_MODE;
import static at.hannibal2.skyhanni.config.features.mining.powdertracker.PowderDisplayEntry.DOUBLE_POWDER;
import static at.hannibal2.skyhanni.config.features.mining.powdertracker.PowderDisplayEntry.ELECTRON;
import static at.hannibal2.skyhanni.config.features.mining.powdertracker.PowderDisplayEntry.FTX;
import static at.hannibal2.skyhanni.config.features.mining.powdertracker.PowderDisplayEntry.GEMSTONE_POWDER;
import static at.hannibal2.skyhanni.config.features.mining.powdertracker.PowderDisplayEntry.GOLD_ESSENCE;
import static at.hannibal2.skyhanni.config.features.mining.powdertracker.PowderDisplayEntry.JADE;
import static at.hannibal2.skyhanni.config.features.mining.powdertracker.PowderDisplayEntry.MITHRIL_POWDER;
import static at.hannibal2.skyhanni.config.features.mining.powdertracker.PowderDisplayEntry.ROBOTRON;
import static at.hannibal2.skyhanni.config.features.mining.powdertracker.PowderDisplayEntry.RUBY;
import static at.hannibal2.skyhanni.config.features.mining.powdertracker.PowderDisplayEntry.SAPPHIRE;
import static at.hannibal2.skyhanni.config.features.mining.powdertracker.PowderDisplayEntry.SPACER_1;
import static at.hannibal2.skyhanni.config.features.mining.powdertracker.PowderDisplayEntry.SPACER_2;
import static at.hannibal2.skyhanni.config.features.mining.powdertracker.PowderDisplayEntry.TITLE;
import static at.hannibal2.skyhanni.config.features.mining.powdertracker.PowderDisplayEntry.TOPAZ;
import static at.hannibal2.skyhanni.config.features.mining.powdertracker.PowderDisplayEntry.TOTAL_CHESTS;

public class PowderTrackerConfig {

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
    @ConfigEditorDraggableList()
    public Property<List<PowderDisplayEntry>> textFormat = Property.of(new ArrayList<>(Arrays.asList(
        TITLE,
        DISPLAY_MODE,
        TOTAL_CHESTS,
        DOUBLE_POWDER,
        MITHRIL_POWDER,
        GEMSTONE_POWDER,
        SPACER_1,
        DIAMOND_ESSENCE,
        GOLD_ESSENCE,
        SPACER_2,
        RUBY,
        SAPPHIRE,
        AMBER,
        AMETHYST,
        JADE,
        TOPAZ,
        FTX,
        ELECTRON,
        ROBOTRON
    )));

    @Expose
    public Position position = new Position(-274, 0, false, true);

}
