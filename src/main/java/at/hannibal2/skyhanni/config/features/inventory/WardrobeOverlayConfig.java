package at.hannibal2.skyhanni.config.features.inventory;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class WardrobeOverlayConfig {

    @Expose
    @ConfigOption(name = "enble", desc = "")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "eyes look mouse", desc = "")
    @ConfigEditorBoolean
    public boolean eyesFollowMouse = true;
}
