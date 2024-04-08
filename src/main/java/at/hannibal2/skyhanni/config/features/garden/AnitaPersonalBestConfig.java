package at.hannibal2.skyhanni.config.features.garden;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class AnitaPersonalBestConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show your your personal best for the current active crop (held tool)")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "", desc = "")
    @ConfigEditorBoolean
    public boolean showBonus = false;

    @Expose
    @ConfigOption(name = "", desc = "")
    @ConfigEditorBoolean
    public boolean onlyInContest = false;

    @Expose
    public Position position = new Position(100, 100, false, true);

}
