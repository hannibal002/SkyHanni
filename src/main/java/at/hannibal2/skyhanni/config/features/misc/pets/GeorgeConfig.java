package at.hannibal2.skyhanni.config.features.misc.pets;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.notenoughupdates.core.config.annotations.ConfigEditorBoolean;
import io.github.moulberry.notenoughupdates.core.config.annotations.ConfigOption;

public class GeorgeConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the currently lowest prices of the pets you need to buy to increase your Taming level cap.")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Other Rarities", desc = "Searches for other rarities besides the ones specified by George.")
    @ConfigEditorBoolean
    public boolean otherRarities = false;

    @Expose
    public Position position = new Position(100, 90, false, true);
}
