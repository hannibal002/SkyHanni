package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class TrackerConfig {

    @Expose
    @ConfigOption(name = "Hide with Item Value", desc = "Hide all trackers while the Estimated Item Value is visible.")
    @ConfigEditorBoolean
    public boolean hideInEstimatedItemValue = true;

    @Expose
    @ConfigOption(name = "Default Display Mode", desc = "Change the display mode that gets shown when starting.")
    @ConfigEditorDropdown
    public Property<SkyHanniTracker.DefaultDisplayMode> defaultDisplayMode = Property.of(SkyHanniTracker.DefaultDisplayMode.TOTAL);
}
