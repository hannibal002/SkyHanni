package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class RiftConfig {

    @ConfigOption(name = "Rift Timer", desc = "")
    @Accordion
    @Expose
    public TimerConfig timer = new TimerConfig();

    public static class TimerConfig {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Show the remaining rift time, max time, percentage, and extra time changes.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Max time", desc = "Show max time.")
        @ConfigEditorBoolean
        public boolean maxTime = true;

        @Expose
        @ConfigOption(name = "Percentage", desc = "Show percentage.")
        @ConfigEditorBoolean
        public boolean percentage = true;

        @Expose
        public Position timerPosition = new Position(10, 10, false, true);
    }

    @ConfigOption(name = "Crux Warnings", desc = "")
    @Accordion
    @Expose
    public CruxWarnings crux = new CruxWarnings();

    public static class CruxWarnings {

        @Expose
        @ConfigOption(name = "Shy Warning Title", desc = "Shows an extra warning when a shy is going to steal your Rift Time.")
        @ConfigEditorBoolean
        public boolean shyWarning = true;
    }
}
