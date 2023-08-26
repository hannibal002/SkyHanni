package at.hannibal2.skyhanni.config.features;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class KuudraConfig {

    @Expose
    @ConfigOption(name = "Kuudra Chest Waypoints", desc = "Render Waypoints to the kuudra chests")
    @ConfigEditorBoolean
    public boolean chestWaypoints = false;

}
