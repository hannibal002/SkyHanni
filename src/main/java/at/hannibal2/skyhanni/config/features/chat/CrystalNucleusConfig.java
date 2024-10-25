package at.hannibal2.skyhanni.config.features.chat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CrystalNucleusConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Hide or compact messages relating to Crystal Nucleus runs in the Crystal Hollows.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    public enum CrystalNucleusMessageTypes {
        RUN_COMPLETED("§5Run Completed Summary"),
        CRYSTAL_COLLECTED("§aC§6r§dy§bs§et§aa§6l §7Collected"),
        CRYSTAL_PLACED("§aC§6r§dy§bs§et§aa§6l §7Placed"),
        NPC_DIVAN_KEEPERS("§e[NPC] §6Keepers §7(§2Mines of Divan§7)"),
        NPC_PROF_ROBOT("§e[NPC] Professor Robot"),
        NPC_KING_YOLKAR("§e[NPC] §6King Yolkar"),
        NPC_GOBLIN_GUARDS("§c[GUARD]§7s (§6Goblin Den§7)"),
        NON_TOOL_SCAVENGE("§7Non-Tool §cMetal Detector §7loot")
        ;

        private final String name;

        CrystalNucleusMessageTypes(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Expose
    @ConfigOption(name = "Modified Messages", desc = "Messages that should be hidden or compacted.\n§cImportant information will still appear§7.")
    @ConfigEditorDraggableList
    public List<CrystalNucleusMessageTypes> modifiedMessages = new ArrayList<>(Arrays.asList(
        CrystalNucleusMessageTypes.CRYSTAL_COLLECTED,
        CrystalNucleusMessageTypes.CRYSTAL_PLACED,
        CrystalNucleusMessageTypes.RUN_COMPLETED,
        CrystalNucleusMessageTypes.NPC_DIVAN_KEEPERS,
        CrystalNucleusMessageTypes.NPC_PROF_ROBOT,
        CrystalNucleusMessageTypes.NPC_KING_YOLKAR,
        CrystalNucleusMessageTypes.NPC_GOBLIN_GUARDS,
        CrystalNucleusMessageTypes.NON_TOOL_SCAVENGE
    ));
}
