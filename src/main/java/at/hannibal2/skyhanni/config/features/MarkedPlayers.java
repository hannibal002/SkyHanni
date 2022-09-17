package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.annotations.ConfigEditorBoolean;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigOption;
import com.google.gson.annotations.Expose;

public class MarkedPlayers {

    @Expose
    @ConfigOption(name = "Highlight in World", desc = "Highlight marked players in the world.")
    @ConfigEditorBoolean
    public boolean highlightInWorld = true;

    @Expose
    @ConfigOption(name = "Highlight in Chat", desc = "Highlight marked player names in chat.")
    @ConfigEditorBoolean
    public boolean highlightInChat = true;

    @Expose
    @ConfigOption(name = "Mark Own Name", desc = "Mark own player name.")
    @ConfigEditorBoolean(runnableId = "markOwnPlayer")
    public boolean markOwnName = false;
}
