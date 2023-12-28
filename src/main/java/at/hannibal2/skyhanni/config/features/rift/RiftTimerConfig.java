package at.hannibal2.skyhanni.config.features.rift;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class RiftTimerConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the remaining rift time, max time, percentage, and extra time changes.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Max Time", desc = "Show max time.")
    @ConfigEditorBoolean
    public Property<Boolean> maxTime = Property.of(true);

    @Expose
    @ConfigOption(name = "Percentage", desc = "Show percentage.")
    @ConfigEditorBoolean
    public Property<Boolean> percentage = Property.of(true);

    @Expose
    public Position timerPosition = new Position(10, 10, false, true);

}
