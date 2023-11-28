package at.hannibal2.skyhanni.config.features.garden.visitor.dropstatistics;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static at.hannibal2.skyhanni.config.features.garden.visitor.dropstatistics.DropsStatisticsTextEntry.ACCEPTED;
import static at.hannibal2.skyhanni.config.features.garden.visitor.dropstatistics.DropsStatisticsTextEntry.COINS_SPENT;
import static at.hannibal2.skyhanni.config.features.garden.visitor.dropstatistics.DropsStatisticsTextEntry.COPPER;
import static at.hannibal2.skyhanni.config.features.garden.visitor.dropstatistics.DropsStatisticsTextEntry.DEDICATION_IV;
import static at.hannibal2.skyhanni.config.features.garden.visitor.dropstatistics.DropsStatisticsTextEntry.DENIED;
import static at.hannibal2.skyhanni.config.features.garden.visitor.dropstatistics.DropsStatisticsTextEntry.FARMING_EXP;
import static at.hannibal2.skyhanni.config.features.garden.visitor.dropstatistics.DropsStatisticsTextEntry.GREEN_BANDANA;
import static at.hannibal2.skyhanni.config.features.garden.visitor.dropstatistics.DropsStatisticsTextEntry.OVERGROWN_GRASS;
import static at.hannibal2.skyhanni.config.features.garden.visitor.dropstatistics.DropsStatisticsTextEntry.SPACER_1;
import static at.hannibal2.skyhanni.config.features.garden.visitor.dropstatistics.DropsStatisticsTextEntry.TITLE;
import static at.hannibal2.skyhanni.config.features.garden.visitor.dropstatistics.DropsStatisticsTextEntry.TOTAL_VISITORS;
import static at.hannibal2.skyhanni.config.features.garden.visitor.dropstatistics.DropsStatisticsTextEntry.VISITORS_BY_RARITY;

public class DropsStatisticsConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Tallies up statistic about visitors and the rewards you have received from them."
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
    public List<DropsStatisticsTextEntry> textFormat = new ArrayList<>(Arrays.asList(
        TITLE,
        TOTAL_VISITORS,
        VISITORS_BY_RARITY,
        ACCEPTED,
        DENIED,
        SPACER_1,
        COPPER,
        FARMING_EXP,
        COINS_SPENT,
        OVERGROWN_GRASS,
        GREEN_BANDANA,
        DEDICATION_IV
    ));

    @Expose
    @ConfigOption(name = "Display Numbers First", desc = "Determines whether the number or drop name displays first. " +
        "§eNote: Will not update the preview above!")
    @ConfigEditorBoolean
    public boolean displayNumbersFirst = true;

    @Expose
    @ConfigOption(name = "Display Icons", desc = "Replaces the drop names with icons. " +
        "§eNote: Will not update the preview above!")
    @ConfigEditorBoolean
    public boolean displayIcons = false;

    @Expose
    @ConfigOption(name = "Only on Barn Plot", desc = "Only shows the overlay while on the Barn plot.")
    @ConfigEditorBoolean
    public boolean onlyOnBarn = true;

    @Expose
    public Position pos = new Position(5, 20, false, true);
}
