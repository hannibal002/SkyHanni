package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class MineshaftWaypointsConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Enables Corpse coordinates.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Show Entrance", desc = "Sets a waypoint on the mineshaft's entrance")
    @ConfigEditorBoolean
    public boolean showEntrance = true;

    @Expose
    @ConfigOption(name = "Share with Party", desc = "Sends waypoint coordinates to chat.")
    @ConfigEditorBoolean
    public boolean sendChat = true;

    @Expose
    @ConfigOption(name = "Receive from Party", desc = "Receives waypoint coordinates and types from chat.")
    @ConfigEditorBoolean
    public boolean receiveChat = true;

    @Expose
    @ConfigOption(name = "Draw Text", desc = "Draws text near the waypoints.")
    @ConfigEditorBoolean
    public boolean drawText = false;

    @Expose
    @ConfigOption(name = "Hide collected", desc = "Hides waypoints for collected corpses.")
    @ConfigEditorBoolean
    public boolean delete = true;

    @Expose
    @ConfigOption(name = "Hide Message", desc = "Hides waypoint messages from the party chat.")
    @ConfigEditorBoolean
    public boolean hideMessage = false;
}
