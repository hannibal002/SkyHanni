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
    @ConfigOption(name = "Enabled", desc = "Modify or hide messages relating to Crystal Nucleus runs in the Crystal Hollows.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    public enum CrystalNucleusMessageTypes {
        RUN_COMPLETED("§5Run Completed"),
        CRYSTAL_COLLECTED("§aC§6r§dy§bs§et§aa§6l §7Collected"),
        CRYSTAL_PLACED("§aC§6r§dy§bs§et§aa§6l §fPlaced"),
        NPC_KEEPER("§e[NPC] §6Keeper"),
        NPC_PROF_ROBOT("§e[NPC] Professor Robot"),
        NPC_KING_YOLKAR("§e[NPC] §6King Yolkar"),
        NON_TOOL_SCAVENGE("§7Non-Tool §cMetal Detector")
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
    @ConfigOption(name = "Hidden Messages", desc = "Messages that should be completely hidden.")
    @ConfigEditorDraggableList
    public List<CrystalNucleusMessageTypes> hiddenMessages = new ArrayList<>(Arrays.asList(
        CrystalNucleusMessageTypes.CRYSTAL_PLACED,
        CrystalNucleusMessageTypes.NON_TOOL_SCAVENGE
    ));

    @Expose
    @ConfigOption(name = "Compacted Messages", desc = "Messages that should be compacted (where possible).")
    @ConfigEditorDraggableList
    public List<CrystalNucleusMessageTypes> compactedMessages = new ArrayList<>(Arrays.asList(
       CrystalNucleusMessageTypes.CRYSTAL_COLLECTED,
       CrystalNucleusMessageTypes.RUN_COMPLETED
    ));
}
