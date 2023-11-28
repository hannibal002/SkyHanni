package at.hannibal2.skyhanni.config.features.event.winter.frozentreasure;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static at.hannibal2.skyhanni.config.features.event.winter.frozentreasure.FrozenTreasureDisplayEntry.COMPACT_PROCS;
import static at.hannibal2.skyhanni.config.features.event.winter.frozentreasure.FrozenTreasureDisplayEntry.ENCHANTED_ICE;
import static at.hannibal2.skyhanni.config.features.event.winter.frozentreasure.FrozenTreasureDisplayEntry.ENCHANTED_PACKED_ICE;
import static at.hannibal2.skyhanni.config.features.event.winter.frozentreasure.FrozenTreasureDisplayEntry.GLACIAL_FRAGMENT;
import static at.hannibal2.skyhanni.config.features.event.winter.frozentreasure.FrozenTreasureDisplayEntry.GLACIAL_TALISMAN;
import static at.hannibal2.skyhanni.config.features.event.winter.frozentreasure.FrozenTreasureDisplayEntry.GREEN_GIFT;
import static at.hannibal2.skyhanni.config.features.event.winter.frozentreasure.FrozenTreasureDisplayEntry.ICE_PER_HOUR;
import static at.hannibal2.skyhanni.config.features.event.winter.frozentreasure.FrozenTreasureDisplayEntry.RED_GIFT;
import static at.hannibal2.skyhanni.config.features.event.winter.frozentreasure.FrozenTreasureDisplayEntry.SPACER_1;
import static at.hannibal2.skyhanni.config.features.event.winter.frozentreasure.FrozenTreasureDisplayEntry.TITLE;
import static at.hannibal2.skyhanni.config.features.event.winter.frozentreasure.FrozenTreasureDisplayEntry.TOTAL_ICE;
import static at.hannibal2.skyhanni.config.features.event.winter.frozentreasure.FrozenTreasureDisplayEntry.TREASURES_MINED;
import static at.hannibal2.skyhanni.config.features.event.winter.frozentreasure.FrozenTreasureDisplayEntry.WHITE_GIFT;

public class FrozenTreasureConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Tracks all of your drops from Frozen Treasure in the Glacial Caves.\n" +
            "§eIce calculations are an estimate but are relatively accurate."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(
        name = "Text Format",
        desc = "Drag text to change the appearance of the overlay."
    )
    @ConfigEditorDraggableList()
    public List<FrozenTreasureDisplayEntry> textFormat = new ArrayList<>(Arrays.asList(
        TITLE,
        TREASURES_MINED,
        TOTAL_ICE,
        ICE_PER_HOUR,
        COMPACT_PROCS,
        SPACER_1,
        WHITE_GIFT,
        GREEN_GIFT,
        RED_GIFT,
        ENCHANTED_ICE,
        ENCHANTED_PACKED_ICE,
        GLACIAL_FRAGMENT,
        GLACIAL_TALISMAN
    ));

    @Expose
    @ConfigOption(name = "Only in Glacial Cave", desc = "Only shows the overlay while in the Glacial Cave.")
    @ConfigEditorBoolean
    public boolean onlyInCave = true;

    @Expose
    @ConfigOption(name = "Show as Drops", desc = "Multiplies the numbers on the display by the base drop. \n" +
        "E.g. 3 Ice Bait -> 48 Ice Bait")
    @ConfigEditorBoolean
    public boolean showAsDrops = false;

    @Expose
    @ConfigOption(name = "Hide Chat Messages", desc = "Hides the chat messages from Frozen Treasures.")
    @ConfigEditorBoolean
    public boolean hideMessages = false;

    @Expose
    public Position position = new Position(10, 80, false, true);
}
