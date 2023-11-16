package at.hannibal2.skyhanni.config.features.chat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class PlayerMessagesConfig {

    @Expose
    @ConfigOption(name = "Player Rank Hider", desc = "Hide player ranks in all chat messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean playerRankHider = false;

    @Expose
    @ConfigOption(name = "Chat Filter", desc = "Scan messages sent by players for blacklisted words and gray out the message if any are found.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean chatFilter = false;
}
