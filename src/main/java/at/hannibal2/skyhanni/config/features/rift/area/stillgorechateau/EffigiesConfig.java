package at.hannibal2.skyhanni.config.features.rift.area.stillgorechateau;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class EffigiesConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show locations of inactive Blood Effigies.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Respawning Soon", desc = "Show effigies that are about to respawn.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean respawningSoon = false;

    @Expose
    @ConfigOption(name = "Respawning Time", desc = "Time before effigies respawn to show.")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 15,
        minStep = 1
    )
    public int respwningSoonTime = 3;

    @Expose
    @ConfigOption(name = "Unknown Times", desc = "Show effigies without known time.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean unknownTime = false;
}
