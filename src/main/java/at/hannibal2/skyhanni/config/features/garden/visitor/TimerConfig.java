package at.hannibal2.skyhanni.config.features.garden.visitor;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class TimerConfig {
    @Expose
    @ConfigOption(name = "Visitor Timer", desc = "Timer when the next visitor will appear, " +
        "and a number for how many visitors are already waiting.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Sixth Visitor Estimate", desc = "Estimate when the sixth visitor in the queue will arrive. " +
        "May be inaccurate with co-op members farming simultaneously.")
    @ConfigEditorBoolean
    public boolean sixthVisitorEnabled = true;

    @Expose
    @ConfigOption(name = "Sixth Visitor Warning", desc = "Notifies when it is believed that the sixth visitor has arrived. " +
        "May be inaccurate with co-op members farming simultaneously.")
    @ConfigEditorBoolean
    public boolean sixthVisitorWarning = true;

    @Expose
    @ConfigOption(name = "New Visitor Ping", desc = "Pings you when you are less than 10 seconds away from getting a new visitor. " +
        "§eUseful for getting Ephemeral Gratitudes during the 2023 Halloween event.")
    @ConfigEditorBoolean
    public boolean newVisitorPing = false;

    @Expose
    public Position pos = new Position(390, 65, false, true);
}
