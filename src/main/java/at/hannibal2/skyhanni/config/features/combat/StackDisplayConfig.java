package at.hannibal2.skyhanni.config.features.combat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class StackDisplayConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Display the number of stacks on armor pieces like Crimson, Terror, Aurora etc.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigLink(owner = StackDisplayConfig.class, field = "enabled")
    public Position position = new Position(480, -210, 1.9f);

    @Expose
    @ConfigOption(name = "Show Timer", desc = "Displays a timer that shows when you lose a stack.")
    @ConfigEditorBoolean
    public boolean showTimer = true;

    @Expose
    @ConfigLink(owner = StackDisplayConfig.class, field = "enabled")
    public Position timerPosition = new Position(466, -190, 1.9f);
}
