package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.annotations.ConfigEditorBoolean;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigEditorButton;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigEditorDropdown;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigOption;
import com.google.gson.annotations.Expose;

public class Update {

    @Expose
    @ConfigOption(name = "Update Stream", desc = "Beta Updates are more unstable, but also more frequent")
    @ConfigEditorDropdown(values = {"None", "Beta", "Full-Release"})
    public int updateStream = 2;

    @Expose
    @ConfigOption(name = "Background Downloads", desc = "Automatically download updates in the background")
    @ConfigEditorBoolean
    public boolean autoDownload = false;

    @Expose
    @ConfigOption(name = "Background Installation", desc = "Automatically install updates in the background")
    @ConfigEditorBoolean
    public boolean autoInstall = false;

    @ConfigOption(name = "Check Now!", desc = "Check for updates now")
    @ConfigEditorButton(runnableId = "updateCheck", buttonText = "Update!")
    public boolean updateCheck;

}
