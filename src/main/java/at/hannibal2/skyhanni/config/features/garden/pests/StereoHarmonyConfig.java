package at.hannibal2.skyhanni.config.features.garden.pests;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class StereoHarmonyConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Shows a display of what pest is being boosted by your vinyl."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean displayEnabled = true;

    @Expose
    @ConfigOption(name = "Show Pest Head", desc = "")
    @ConfigEditorBoolean
    public Property<Boolean> showHead = Property.of(true);

    @Expose
    @ConfigOption(
        name = "Hide when None",
        desc = "Hide when no vinyl selected."
    )
    @ConfigEditorBoolean
    public boolean hideWhenNone = false;

    @Expose
    public Position position = new Position(205, 55, 1f);
}
