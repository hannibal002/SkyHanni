package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class RiftConfig {

    @ConfigOption(name = "Rift Timer", desc = "")
    @Accordion
    @Expose
    public RiftTimerConfig timer = new RiftTimerConfig();

    public static class RiftTimerConfig {

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

    @ConfigOption(name = "Crux Features", desc = "")
    @Accordion
    @Expose
    public CruxWarnings crux = new CruxWarnings();

    public static class CruxWarnings {

        @Expose
        @ConfigOption(name = "Shy Warning", desc = "Shows a warning when a shy is going to steal your time. " +
                "Useful if you play without volume.")
        @ConfigEditorBoolean
        public boolean shyWarning = true;


        @Expose
        @ConfigOption(name = "Volt Warning", desc = "Shows a warning while a volt is discharging lightning.")
        @ConfigEditorBoolean
        public boolean voltWarning = true;

        @Expose
        @ConfigOption(name = "Volt Range Highlighter", desc = "Shows the area in which a Volt might strike lightning.")
        @ConfigEditorBoolean
        public boolean voltRange = true;

        @Expose
        @ConfigOption(name = "Volt Range Highlighter Color", desc = "In which color should the volt range be highlighted?")
        @ConfigEditorColour
        public String voltColour = "0:60:0:0:255";

        @Expose
        @ConfigOption(name = "Volt mood color", desc = "Change the color of the volt enemy depending on their mood.")
        @ConfigEditorBoolean
        public boolean voltMoodMeter = false;

        @Expose
        @ConfigOption(name = "Crux Talisman Display", desc = "Display progress of the Crux Talisman on screen.")
        @ConfigEditorBoolean
        public boolean cruxTalismanProgress = true;

        @Expose
        public Position cruxTalismanPosition = new Position(144, 139, false, true);
    }

    @ConfigOption(name = "Larvas", desc = "")
    @Accordion
    @Expose
    public LarvasConfig larvas = new LarvasConfig();

    public static class LarvasConfig {

        @Expose
        @ConfigOption(name = "Highlight", desc = "Highlight §cLarvas on trees §7while holding a §eLarva Hook §7in the hand.")
        @ConfigEditorBoolean
        public boolean highlight = true;

        @Expose
        @ConfigOption(name = "Color", desc = "Color of the Larvas.")
        @ConfigEditorColour
        public String highlightColor = "0:120:13:49:255";

    }

    @ConfigOption(name = "Odonatas", desc = "")
    @Accordion
    @Expose
    public OdonataConfig odonata = new OdonataConfig();

    public static class OdonataConfig {

        @Expose
        @ConfigOption(name = "Highlight", desc = "Highlight the small §cOdonatas §7flying around the trees while holding a §eEmpty Odonata Bottle §7in the hand.")
        @ConfigEditorBoolean
        public boolean highlight = true;

        @Expose
        @ConfigOption(name = "Color", desc = "Color of the Odonatas.")
        @ConfigEditorColour
        public String highlightColor = "0:120:13:49:255";

    }

    @Expose
    @ConfigOption(name = "Highlight Guide", desc = "Highlight things to do in the Rift Guide.")
    @ConfigEditorBoolean
    public boolean highlightGuide = true;

    @Expose
    @ConfigOption(name = "Agaricus Cap", desc = "Counts down the time until §eAgaricus Cap (Mushroom) §7changes color from brown to red and is breakable.")
    @ConfigEditorBoolean
    public boolean agaricusCap = true;
}
