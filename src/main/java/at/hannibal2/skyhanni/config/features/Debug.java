package at.hannibal2.skyhanni.config.features;


import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigEditorBoolean;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigEditorButton;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigOption;
import com.google.gson.annotations.Expose;

public class Debug {

    @Expose
    @ConfigOption(name = "Enable Test", desc = "Enable Test logic")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Test Location", desc = "")
    @ConfigEditorButton(runnableId = "testPos", buttonText = "Edit")
    public Position testPos = new Position(10, 10, false, true);
}