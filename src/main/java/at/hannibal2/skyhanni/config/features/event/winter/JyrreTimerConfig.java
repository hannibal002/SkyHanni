package at.hannibal2.skyhanni.config.features.event.winter;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class JyrreTimerConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "A timer showing the remaining duration of your intelligence boost")
    @ConfigEditorBoolean
    @FeatureToggle
    public Property<Boolean> enabled = Property.of(false);

    @Expose
    @ConfigOption(name = "Show when Inactive", desc = "Show the timer when inactive, rather than removing it")
    @ConfigEditorBoolean
    public Property<Boolean> showInactive = Property.of(true);

    @Expose
    public Position pos = new Position(390, 65, false, true);
}
