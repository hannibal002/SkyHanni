package at.hannibal2.skyhanni.config.features.event.diana;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class MythologicalMobTrackerConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Counts the different mythological mobs you have dug up.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    public Position position = new Position(20, 20, false, true);

    @Expose
    @ConfigOption(name = "Show Percentage", desc = "Show percentage how often what mob spawned.")
    @ConfigEditorBoolean
    public Property<Boolean> showPercentage = Property.of(false);

    @Expose
    @ConfigOption(name = "Hide Chat", desc = "Hide the chat messages when digging up a mythological mob.")
    @ConfigEditorBoolean
    public boolean hideChat = false;
}
