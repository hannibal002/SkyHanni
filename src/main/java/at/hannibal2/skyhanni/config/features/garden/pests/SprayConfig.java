package at.hannibal2.skyhanni.config.features.garden.pests;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class SprayConfig {

    @Expose
    @ConfigOption(
        name = "Pest Spray Selector",
        desc = "Show the pests that are attracted when changing the selected material of the §aSprayanator§7."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean pestWhenSelector = true;

    @Expose
    public Position position = new Position(315, -200, 2.3f);
}
