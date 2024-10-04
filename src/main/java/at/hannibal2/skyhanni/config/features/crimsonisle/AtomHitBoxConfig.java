package at.hannibal2.skyhanni.config.features.crimsonisle;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AtomHitBoxConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show Exes, Wais and Zees HitBox.")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(
        name = "Select Atoms",
        desc = "Select atoms you want to show the hitbox for .."
    )
    @ConfigEditorDraggableList
    public List<AtomsEntries> atomsEntries = new ArrayList<>(Arrays.asList(
        AtomsEntries.EXE,
        AtomsEntries.WAI,
        AtomsEntries.ZEE
    ));

    public enum AtomsEntries {
        EXE("§aExes"),
        WAI("§6Wais"),
        ZEE("§5Zees"),
        ;

        private final String str;
        AtomsEntries(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
