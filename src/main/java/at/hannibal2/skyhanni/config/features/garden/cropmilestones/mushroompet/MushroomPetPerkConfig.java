package at.hannibal2.skyhanni.config.features.garden.cropmilestones.mushroompet;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static at.hannibal2.skyhanni.config.features.garden.cropmilestones.mushroompet.MushroomTextEntry.MUSHROOM_TIER;
import static at.hannibal2.skyhanni.config.features.garden.cropmilestones.mushroompet.MushroomTextEntry.NUMBER_OUT_OF_TOTAL;
import static at.hannibal2.skyhanni.config.features.garden.cropmilestones.mushroompet.MushroomTextEntry.TIME;
import static at.hannibal2.skyhanni.config.features.garden.cropmilestones.mushroompet.MushroomTextEntry.TITLE;

// TODO moulconfig runnable support
public class MushroomPetPerkConfig {
    @Expose
    @ConfigOption(
        name = "Display Enabled",
        desc = "Show the progress and ETA for mushroom crops when farming other crops because of the Mooshroom Cow perk.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(
        name = "Mushroom Text",
        desc = "Drag text to change the appearance of the overlay.\n" +
            "Hold a farming tool to show the overlay."
    )
    @ConfigEditorDraggableList()
    public List<MushroomTextEntry> text = new ArrayList<>(Arrays.asList(
        TITLE,
        MUSHROOM_TIER,
        NUMBER_OUT_OF_TOTAL,
        TIME
    ));

    @Expose
    public Position pos = new Position(-112, -143, false, true);
}
