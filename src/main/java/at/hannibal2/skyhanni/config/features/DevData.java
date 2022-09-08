package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.gui.core.config.Position;
import at.hannibal2.skyhanni.config.gui.core.config.annotations.*;
import com.google.gson.annotations.Expose;

public class DevData {

    @Expose
    @ConfigOption(name = "Repo Auto Update", desc = "Update the repository on every startup.")
    @ConfigEditorBoolean
    public boolean repoAutoUpdate = true;

    @Expose
    @ConfigOption(name = "Debug", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean debugDO_NOT_USE = false;

    @Expose
    @ConfigOption(name = "Enable Debug", desc = "Enable Test logic")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean debugEnabled = false;

    @Expose
    @ConfigOption(name = "Debug Location", desc = "")
    @ConfigEditorButton(runnableId = "debugPos", buttonText = "Edit")
    @ConfigAccordionId(id = 0)
    public Position debugPos = new Position(10, 10, false, true);
}
