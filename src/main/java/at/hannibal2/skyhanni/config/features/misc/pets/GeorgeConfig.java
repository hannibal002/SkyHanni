package at.hannibal2.skyhanni.config.features.misc.pets;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class GeorgeConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the currently lowest prices of the pets you need to increase your Taming level cap.")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    public Position pos = new Position(100, 90, false, true);
}
