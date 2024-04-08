package at.hannibal2.skyhanni.config.features.garden;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class AnitaPersonalBestConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show your personal best for the current active crop (held tool)")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Show Bonus", desc = "Show your current farming fortune bonus.")
    @ConfigEditorBoolean
    public boolean showBonus = false;

    @Expose
    @ConfigOption(name = "Only In Contest", desc = "Show only when a contest is active.")
    @ConfigEditorBoolean
    public boolean onlyInContest = false;

    @Expose
    public Position position = new Position(-405, 3, false, true);
}
