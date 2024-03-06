package at.hannibal2.skyhanni.config.features.event;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class CenturyConfig {

    @ConfigOption(name = "Enable Active Player Timer", desc = "Show a HUD telling you how much longer you have to wait to be eligible for another free ticket.")
    @Expose
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enableActiveTimer = true;

    @Expose
    public Position activeTimerPosition = new Position(100, 100, false, true);

    @ConfigOption(name = "Enable Active Player Alert", desc = "Loudly proclaim when it is time to break some wheat.")
    @Expose
    @ConfigEditorBoolean
    public boolean enableActiveAlert = false;
}
