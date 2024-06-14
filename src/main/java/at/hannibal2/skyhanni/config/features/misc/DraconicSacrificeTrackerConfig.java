package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static at.hannibal2.skyhanni.config.features.misc.DraconicSacrificeTrackerConfig.DraconicSacrificeDisplayEntry.DRAGON_HORN;
import static at.hannibal2.skyhanni.config.features.misc.DraconicSacrificeTrackerConfig.DraconicSacrificeDisplayEntry.EMPTY_2;
import static at.hannibal2.skyhanni.config.features.misc.DraconicSacrificeTrackerConfig.DraconicSacrificeDisplayEntry.EMPTY_1;
import static at.hannibal2.skyhanni.config.features.misc.DraconicSacrificeTrackerConfig.DraconicSacrificeDisplayEntry.ESSENCES;
import static at.hannibal2.skyhanni.config.features.misc.DraconicSacrificeTrackerConfig.DraconicSacrificeDisplayEntry.HOLY_DRAGON_FRAGMENT;
import static at.hannibal2.skyhanni.config.features.misc.DraconicSacrificeTrackerConfig.DraconicSacrificeDisplayEntry.ITEMS_SACRIFICE;
import static at.hannibal2.skyhanni.config.features.misc.DraconicSacrificeTrackerConfig.DraconicSacrificeDisplayEntry.OLD_DRAGON_FRAGMENT;
import static at.hannibal2.skyhanni.config.features.misc.DraconicSacrificeTrackerConfig.DraconicSacrificeDisplayEntry.PROTECTOR_DRAGON_FRAGMENT;
import static at.hannibal2.skyhanni.config.features.misc.DraconicSacrificeTrackerConfig.DraconicSacrificeDisplayEntry.RITUAL_RESIDUE;
import static at.hannibal2.skyhanni.config.features.misc.DraconicSacrificeTrackerConfig.DraconicSacrificeDisplayEntry.STRONG_DRAGON_FRAGMENT;
import static at.hannibal2.skyhanni.config.features.misc.DraconicSacrificeTrackerConfig.DraconicSacrificeDisplayEntry.SUMMONING_EYE;
import static at.hannibal2.skyhanni.config.features.misc.DraconicSacrificeTrackerConfig.DraconicSacrificeDisplayEntry.SUPERIOR_DRAGON_FRAGMENT;
import static at.hannibal2.skyhanni.config.features.misc.DraconicSacrificeTrackerConfig.DraconicSacrificeDisplayEntry.TITLE;
import static at.hannibal2.skyhanni.config.features.misc.DraconicSacrificeTrackerConfig.DraconicSacrificeDisplayEntry.UNSTABLE_DRAGON_FRAGMENT;
import static at.hannibal2.skyhanni.config.features.misc.DraconicSacrificeTrackerConfig.DraconicSacrificeDisplayEntry.WISE_DRAGON_FRAGMENT;
import static at.hannibal2.skyhanni.config.features.misc.DraconicSacrificeTrackerConfig.DraconicSacrificeDisplayEntry.YOUNG_DRAGON_FRAGMENT;

public class DraconicSacrificeTrackerConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Enable the Draconic sacrifice tracker.")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Only In Void Slate", desc = "Show the tracker only when inside the Void Slate area.")
    @ConfigEditorBoolean
    public boolean onlyInVoidSlate = true;

    @Expose
    @ConfigOption(
        name = "Text Format",
        desc = "Drag text to change the appearance of the overlay."
    )
    @ConfigEditorDraggableList
    public Property<List<DraconicSacrificeDisplayEntry>> textFormat = Property.of(new ArrayList<>(Arrays.asList(
        TITLE,
        ITEMS_SACRIFICE,
        ESSENCES,
        EMPTY_1,
        RITUAL_RESIDUE,
        SUMMONING_EYE,
        DRAGON_HORN,
        EMPTY_2,
        YOUNG_DRAGON_FRAGMENT,
        OLD_DRAGON_FRAGMENT,
        STRONG_DRAGON_FRAGMENT,
        WISE_DRAGON_FRAGMENT,
        UNSTABLE_DRAGON_FRAGMENT,
        PROTECTOR_DRAGON_FRAGMENT,
        SUPERIOR_DRAGON_FRAGMENT,
        HOLY_DRAGON_FRAGMENT
    )));

    public enum DraconicSacrificeDisplayEntry {
        TITLE("§5§lDraconic Sacrifice Tracker"),
        ITEMS_SACRIFICE("§6974 Items Sacrified"),
        ESSENCES("§b8,945 §5Dragon Essences"),
        EMPTY_1("§7<EMPTY>"),
        RITUAL_RESIDUE("§b9 §5Ritual Residue"),
        SUMMONING_EYE("§b4 §5Summoning Eye"),
        DRAGON_HORN("§b1 §5Dragon Horn"),
        EMPTY_2("§7<EMPTY>"),
        YOUNG_DRAGON_FRAGMENT("§b99 §5Young Dragon Fragment"),
        OLD_DRAGON_FRAGMENT("§b99 §5Old Dragon Fragment"),
        STRONG_DRAGON_FRAGMENT("§b99 §5Strong Dragon Fragment"),
        WISE_DRAGON_FRAGMENT("§b99 §5Wise Dragon Fragment"),
        UNSTABLE_DRAGON_FRAGMENT("§b99 §5Unstable Dragon Fragment"),
        PROTECTOR_DRAGON_FRAGMENT("§b99 §5Protector Dragon Fragment"),
        SUPERIOR_DRAGON_FRAGMENT("§b99 §5Superior Dragon Fragment"),
        HOLY_DRAGON_FRAGMENT("§b99 §5Holy Dragon Fragment"),
        EMPTY_3("§7<EMPTY>"),
        DRAGON_CLAW("§b1 §5Dragon Claw"),
        ENCHANTED_ENDER_PEARL("§b666 §aEnchanted Ender Pearl"),
        ENCHANTED_EYE_OF_ENDER("§b412 §aEnchanted Eye Of Ender"),
        ENCHANTED_END_STONE("§b50 §aEnchanted End Stone"),
        ENCHANTED_OBSIDIAN("§b40 §aEnchanted Obsidian"),
        ;
        private final String str;

        DraconicSacrificeDisplayEntry(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }


    @Expose
    @ConfigLink(owner = DraconicSacrificeTrackerConfig.class, field = "enabled")
    public Position position = new Position(201, 199, false, true);

}
