package at.hannibal2.skyhanni.config.features.inventory;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ExperimentationTableDisplayConfig {

    @Expose
    @ConfigOption(name = "Show Display", desc = "Shows a display with useful information for the experimentation table while doing experiments.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigLink(owner = ExperimentationTableDisplayConfig.class, field = "enabled")
    public Position informationDisplayPosition = new Position(0, 200, false, true);

    public enum Experiments {
        NONE("", 0),
        BEGINNER("Beginner", 14),
        HIGH("High", 20),
        GRAND("Grand", 20),
        SUPREME("Supreme", 28),
        TRANSCENDENT("Transcendent", 28),
        METAPHYSICAL("Metaphysical", 28),
        ;

        public final String name;
        public final int gridSize;

        Experiments(String name, int gridSize) {
            this.name = name;
            this.gridSize = gridSize;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
