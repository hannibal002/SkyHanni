package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class DraconicSacrificeTrackerConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Enable the Draconic sacrifice tracker.")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Only In Void Slate", desc = "Show the tracker only when inside the Void Slate area.")
    @ConfigEditorBoolean
    public boolean onlyInVoidSlate = true;

    @Expose
    @ConfigLink(owner = DraconicSacrificeTrackerConfig.class, field = "enabled")
    public Position position = new Position(201, 199, false, true);

}
