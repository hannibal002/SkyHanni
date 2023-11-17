package at.hannibal2.skyhanni.config.features.event;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class GreatSpookConfig {

    @Expose
    @ConfigOption(name = "Primal Fear Timer", desc = "Shows cooldown timer for next primal fear.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean primalFearTimer = false;

    @Expose
    @ConfigOption(name = "Primal Fear Notify", desc = "Plays a notification sound when the next primal fear can spawn.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean primalFearNotification = false;

    @Expose
    public Position positionTimer = new Position(20, 20, false, true);

    @Expose
    @ConfigOption(name = "Fear Stat Display", desc = "Shows your current Fear stat value.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean fearStatDisplay = false;

    @Expose
    public Position positionFear = new Position(30, 30, false, true);

    @Expose
    @ConfigOption(name = "IRL Time Left", desc = "Shows the IRL time left before The Great Spook ends.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean greatSpookTimeLeft = false;

    @Expose
    public Position positionTimeLeft = new Position(40, 40, false, true);

}
