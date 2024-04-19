package at.hannibal2.skyhanni.config.features.dungeon;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class MessageFilterConfig {
    @Expose
    @ConfigOption(name = "Keys and Doors", desc = "Hides the chat message when picking up keys or opening doors in Dungeons.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean keysAndDoors = false;
}
