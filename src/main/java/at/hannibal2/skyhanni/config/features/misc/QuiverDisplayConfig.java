package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class QuiverDisplayConfig {
    @Expose
    @ConfigOption(name = "Enable Quiver Display", desc = "Show the number of arrows you have.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Show arrow icon", desc = "")
    @ConfigEditorBoolean
    public boolean showIcon = true;

    @Expose
    @ConfigOption(name = "Show only with bow", desc = "Only show when a bow is in your inventory")
    @ConfigEditorBoolean
    public boolean onlyWithBow = true;

    @Expose
    public Position quiverDisplayPos = new Position(10, 10);
}
