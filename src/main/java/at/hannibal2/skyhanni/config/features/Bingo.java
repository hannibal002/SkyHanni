package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.annotations.ConfigEditorBoolean;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigOption;
import com.google.gson.annotations.Expose;

public class Bingo {

    @Expose
    @ConfigOption(name = "Compact Chat Messages", desc = "Shortens chat messages about skill level ups, collection gains, " +
            "new area discoveries, and bestiarity upgrades while on bingo.")
    @ConfigEditorBoolean
    public boolean compactChatMessages = true;
}
