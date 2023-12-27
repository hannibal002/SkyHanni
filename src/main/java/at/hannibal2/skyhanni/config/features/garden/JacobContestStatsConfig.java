package at.hannibal2.skyhanni.config.features.garden;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JacobContestStatsConfig {

    @Expose
    @ConfigOption(
        name = "Contest Stats",
        desc = "Show contest stats during a contest and after a contest in chat."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean jacobContestSummary = true;

    @Expose
    @ConfigOption(name = "Contest Stats Text", desc = "Drag to change the order of the overlay."
    )
    @ConfigEditorDraggableList
    public List<ContestStatsTextEntry> text = new ArrayList<>(Arrays.asList(
        ContestStatsTextEntry.TITLE,
        ContestStatsTextEntry.TIME_PARTICIPATED,
        ContestStatsTextEntry.BLOCKS_BROKEN,
        ContestStatsTextEntry.BPS,
        ContestStatsTextEntry.POSITION,
        ContestStatsTextEntry.PREDICTED_SCORE
    ));

    public enum ContestStatsTextEntry {
        TITLE("§e§lWheat Contest Stats"),
        TIME_PARTICIPATED("§7Participating for §b18m 23s"),
        BLOCKS_BROKEN("§7Blocks Broken: §e21,972"),
        BPS("§7Blocks per Second: §c19.92"),
        POSITION("§7Estimated Position: §b5th §7§7(Top §b0.4% §7◆ 1.1k)"),
        PREDICTED_SCORE("§7Predicted Score: §e432,123");

        private final String str;

        ContestStatsTextEntry(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    public Position pos = new Position(-112, -143, false, true);
}
