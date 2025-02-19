package at.hannibal2.skyhanni.config.features.dungeon;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class DungeonTrinityHelperConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Notifies user when Trinity is highly likely to appear on dungeon. (Puzzle count == 5)")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Notify party", desc = "Automatically send message to party to watch out for Trinity")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean sendPartyChat = true;

}
