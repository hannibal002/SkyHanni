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
    @ConfigOption(name = "Delete on collect", desc = "Deletes the waypoint after collecting a corpse.")
    @ConfigEditorBoolean
    public boolean delete = true;
}
