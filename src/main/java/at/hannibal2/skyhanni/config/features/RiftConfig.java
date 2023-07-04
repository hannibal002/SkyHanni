package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.*;
import io.github.moulberry.moulconfig.observer.Property;
import scala.tools.nsc.interpreter.Formatting;

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

    @ConfigOption(name = "Crux Warnings", desc = "")
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
    }

    @ConfigOption(name = "Crux Talisman Progress", desc = "")
    @Accordion
    @Expose
    public CruxTalisman cruxTalisman = new CruxTalisman();

    public static class CruxTalisman {
        @Expose
        @ConfigOption(name = "Crux Talisman Display", desc = "Display progress of the Crux Talisman on screen.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Compact", desc = "Show a compacted version of the overlay when the talisman is maxed.")
        @ConfigEditorBoolean
        public boolean compactWhenMaxed = false;

        @Expose
        @ConfigOption(name = "Show Bonuses", desc = "Show bonuses you get from the talisman.")
        @ConfigEditorBoolean
        public Property<Boolean> showBonuses = Property.of(true);

        @Expose
        public Position position = new Position(144, 139, false, true);
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
        @ConfigOption(name = "Highlight", desc = "Highlight the small §cOdonatas §7flying around the trees while holding a " +
                "§eEmpty Odonata Bottle §7in the hand.")
        @ConfigEditorBoolean
        public boolean highlight = true;

        @Expose
        @ConfigOption(name = "Color", desc = "Color of the Odonatas.")
        @ConfigEditorColour
        public String highlightColor = "0:120:13:49:255";

    }

    @ConfigOption(name = "Enigma Soul Waypoints", desc = "")
    @Accordion
    @Expose
    public EnigmaSoulConfig enigmaSoulWaypoints = new EnigmaSoulConfig();

    public static class EnigmaSoulConfig {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Click on Enigma Souls in Rift Guides to highlight their location.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Color", desc = "Color of the Enigma Souls.")
        @ConfigEditorColour
        public String color = "0:120:13:49:255";

    }

    @Expose
    @ConfigOption(name = "Highlight Guide", desc = "Highlight things to do in the Rift Guide.")
    @ConfigEditorBoolean
    public boolean highlightGuide = true;

    @Expose
    @ConfigOption(name = "Agaricus Cap", desc = "Counts down the time until §eAgaricus Cap (Mushroom) " +
            "§7changes color from brown to red and is breakable.")
    @ConfigEditorBoolean
    public boolean agaricusCap = true;

    @ConfigOption(name = "Mirror Verse", desc = "")
    @Accordion
    @Expose
    public MirrorVerse mirrorVerse = new MirrorVerse();

    public static class MirrorVerse {

        @ConfigOption(name = "Dance Room Helper", desc = "")
        @Accordion
        @Expose
        public DanceRoomHelper danceRoomHelper = new DanceRoomHelper();

        public static class DanceRoomHelper{

            @Expose
            @ConfigOption(name= "Enabled", desc = "Helps to solve the dance room in the mirror verse by showing multiple tasks at once.")
            @ConfigEditorBoolean
            public boolean enabled = false;

            @Expose
            @ConfigOption(name = "Lines to show", desc = "How many tasks you should see.")
            @ConfigEditorSlider(minStep = 1, maxValue = 49, minValue = 1)
            public int lineToShow = 3;

            @Expose
            @ConfigOption(name = "Space", desc = "Change the space between each line.")
            @ConfigEditorSlider(minStep = 1, maxValue = 10, minValue = -5)
            public int extraSpace = 0;

            @Expose
            @ConfigOption(name = "Hide others players", desc = "Hide other players inside the dance room.")
            @ConfigEditorBoolean
            public boolean hidePlayers = false;

            @Expose
            @ConfigOption(name = "Hide Title", desc = "Hide Instructions, \"§aIt's happening!\" §7and \"§aKeep it up!\" §7titles.")
            @ConfigEditorBoolean
            public boolean hideOriginalTitle = false;

            @Expose
            @ConfigOption(name = "Formatting", desc = "")
            @Accordion
            public DanceRoomFormatting danceRoomFormatting = new DanceRoomFormatting();

            public static class DanceRoomFormatting {

                @Expose
                @ConfigOption(name = "Now", desc = "Formatting for \"Now:\"")
                @ConfigEditorText
                public String now = "&7Now: ";

                @Expose
                @ConfigOption(name = "Next", desc = "Formatting for \"Next:\"")
                @ConfigEditorText
                public String next = "&7Next: ";

                @Expose
                @ConfigOption(name = "Later", desc = "Formatting for \"Later:\"")
                @ConfigEditorText
                public String later = "&7Later: ";

                @Expose
                @ConfigOption(name = "Color Option", desc = "")
                @Accordion
                public Color color = new Color();
                public static class Color {
                    @Expose
                    @ConfigOption(name = "Move", desc = "Color for the Move instruction")
                    @ConfigEditorText
                    public String move = "&e";

                    @Expose
                    @ConfigOption(name = "Stand", desc = "Color for the Stand instruction")
                    @ConfigEditorText
                    public String stand = "&e";

                    @Expose
                    @ConfigOption(name = "Sneak", desc = "Color for the Sneak instruction")
                    @ConfigEditorText
                    public String sneak = "&5";

                    @Expose
                    @ConfigOption(name = "Jump", desc = "Color for the Jump instruction")
                    @ConfigEditorText
                    public String jump = "&b";

                    @Expose
                    @ConfigOption(name = "Punch", desc = "Color for the Punch instruction")
                    @ConfigEditorText
                    public String punch = "&d";

                    @Expose
                    @ConfigOption(name = "Default", desc = "Fallback color")
                    @ConfigEditorText
                    public String fallback = "&f";
                }
            }

            @Expose
            public Position position = new Position(442, 239, false, true);

        }

        @Expose
        @ConfigOption(name = "Tubulator", desc = "Highlights the location of the invisible blocks (Laser Parkour).")
        @ConfigEditorBoolean
        public boolean laserParkour = true;

    }

    @ConfigOption(name = "Kloon Hacking", desc = "")
    @Accordion
    @Expose
    public KloonHacking hacking = new KloonHacking();

    public static class KloonHacking {

        @Expose
        @ConfigOption(name = "Hacking Solver", desc = "Highlights the correct button to click in the hacking inventory.")
        @ConfigEditorBoolean
        public boolean solver = true;

        @Expose
        @ConfigOption(name = "Color Guide", desc = "Tells you which colour to pick.")
        @ConfigEditorBoolean
        public boolean colour = true;

        @Expose
        @ConfigOption(name = "Terminal Waypoints", desc = "While wearing the helmet, waypoints will appear at each terminal location.")
        @ConfigEditorBoolean
        public boolean waypoints = true;

    }

}
