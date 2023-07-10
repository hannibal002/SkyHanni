package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.*;
import io.github.moulberry.moulconfig.observer.Property;

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

        @ConfigOption(name = "Lava Maze", desc = "")
        @Accordion
        @Expose
        public LavaMazeConfig lavaMazeConfig = new LavaMazeConfig();

        public static class LavaMazeConfig {

            @Expose
            @ConfigOption(name = "Enabled", desc = "Helps solving the lava maze in the mirror verse by showing the correct way.")
            @ConfigEditorBoolean
            public boolean enabled = true;

            @Expose
            @ConfigOption(name = "Look Ahead", desc = "Change how many platforms should be shown in front of you.")
            @ConfigEditorSlider(minStep = 1, maxValue = 30, minValue = 1)
            public Property<Integer> lookAhead = Property.of(3);

            @Expose
            @ConfigOption(name = "Rainbow Color", desc = "Show the rainbow color effect instead of a boring monochrome.")
            @ConfigEditorBoolean
            public Property<Boolean> rainbowColor = Property.of(true);

            @Expose
            @ConfigOption(name = "Monochrome Color", desc = "Set a boring monochrome color for the parkour platforms.")
            @ConfigEditorColour
            public Property<String> monochromeColor = Property.of("0:60:0:0:255");

            @Expose
            @ConfigOption(name = "Hide others players", desc = "Hide other players while doing the lava maze.")
            @ConfigEditorBoolean
            public boolean hidePlayers = false;
        }


        @ConfigOption(name = "Upside Down Parkour", desc = "")
        @Accordion
        @Expose
        public UpsideDownParkour upsideDownParkour = new UpsideDownParkour();

        public static class UpsideDownParkour {

            @Expose
            @ConfigOption(name = "Enabled", desc = "Helps solving the upside down parkour in the mirror verse by showing the correct way.")
            @ConfigEditorBoolean
            public boolean enabled = true;

            @Expose
            @ConfigOption(name = "Look Ahead", desc = "Change how many platforms should be shown in front of you.")
            @ConfigEditorSlider(minStep = 1, maxValue = 9, minValue = 1)
            public Property<Integer> lookAhead = Property.of(3);

            @Expose
            @ConfigOption(name = "Outline", desc = "Outlines the top edge of the platforms.")
            @ConfigEditorBoolean
            public boolean outline = true;

            @Expose
            @ConfigOption(name = "Rainbow Color", desc = "Show the rainbow color effect instead of a boring monochrome.")
            @ConfigEditorBoolean
            public Property<Boolean> rainbowColor = Property.of(true);

            @Expose
            @ConfigOption(name = "Monochrome Color", desc = "Set a boring monochrome color for the parkour platforms.")
            @ConfigEditorColour
            public Property<String> monochromeColor = Property.of("0:60:0:0:255");

            @Expose
            @ConfigOption(name = "Hide others players", desc = "Hide other players while doing the upside down parkour.")
            @ConfigEditorBoolean
            public boolean hidePlayers = false;
        }


        @ConfigOption(name = "Dance Room Helper", desc = "")
        @Accordion
        @Expose
        public DanceRoomHelper danceRoomHelper = new DanceRoomHelper();

        public static class DanceRoomHelper {

            @Expose
            @ConfigOption(name = "Enabled", desc = "Helps to solve the dance room in the mirror verse by showing multiple tasks at once.")
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
                public String now = "&7Now:";

                @Expose
                @ConfigOption(name = "Next", desc = "Formatting for \"Next:\"")
                @ConfigEditorText
                public String next = "&7Next:";

                @Expose
                @ConfigOption(name = "Later", desc = "Formatting for \"Later:\"")
                @ConfigEditorText
                public String later = "&7Later:";

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
                    @ConfigOption(name = "Countdown", desc = "Color for the Countdown")
                    @ConfigEditorText
                    public String countdown = "&f";

                    @Expose
                    @ConfigOption(name = "Default", desc = "Fallback color")
                    @ConfigEditorText
                    public String fallback = "&f";
                }
            }

            @Expose
            public Position position = new Position(442, 239, false, true);
        }


        @ConfigOption(name = "Tubulator", desc = "")
        @Accordion
        @Expose
        public TubulatorConfig tubulatorConfig = new TubulatorConfig();

        public static class TubulatorConfig {

            @Expose
            @ConfigOption(name = "Enabled", desc = "Highlights the location of the invisible Tubulator blocks (Laser Parkour).")
            @ConfigEditorBoolean
            public boolean enabled = true;

            @Expose
            @ConfigOption(name = "Look Ahead", desc = "Change how many platforms should be shown in front of you.")
            @ConfigEditorSlider(minStep = 1, maxValue = 30, minValue = 1)
            public Property<Integer> lookAhead = Property.of(2);

            @Expose
            @ConfigOption(name = "Outline", desc = "Outlines the top edge of the platforms.")
            @ConfigEditorBoolean
            public boolean outline = true;

            @Expose
            @ConfigOption(name = "Rainbow Color", desc = "Show the rainbow color effect instead of a boring monochrome.")
            @ConfigEditorBoolean
            public Property<Boolean> rainbowColor = Property.of(true);

            @Expose
            @ConfigOption(name = "Monochrome Color", desc = "Set a boring monochrome color for the parkour platforms.")
            @ConfigEditorColour
            public Property<String> monochromeColor = Property.of("0:60:0:0:255");

            @Expose
            @ConfigOption(name = "Hide others players", desc = "Hide other players while doing the lava maze.")
            @ConfigEditorBoolean
            public boolean hidePlayers = false;
        }
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

    @Expose
    @ConfigOption(name = "Vampire Slayer Features", desc = "")
    @Accordion
    public VampireSlayerConfig vampireSlayerConfig = new VampireSlayerConfig();

    public static class VampireSlayerConfig {

        @Expose
        @ConfigOption(name = "Your Boss", desc = "")
        @Accordion
        public OwnBoss ownBoss = new OwnBoss();
        public static class OwnBoss {

            @Expose
            @ConfigOption(name = "Highlight your boss", desc = "Highlight your own vampire slayer boss.")
            @ConfigEditorBoolean
            public boolean highlight = true;

            @Expose
            @ConfigOption(name = "Highlight Color", desc = "What color to highlight the boss in.")
            @ConfigEditorColour
            public String highlightColor = "0:249:0:255:88";

            @Expose
            @ConfigOption(name = "Steak Alert", desc = "Show a title when you can steak your boss.")
            @ConfigEditorBoolean
            public boolean steakAlert = true;

            @Expose
            @ConfigOption(name = "Twinclaws warning", desc = "Send a title when Twinclaws is about to happen.\nWork on others highlighted people boss.")
            @ConfigEditorBoolean
            public boolean twinClawsTitle = true;
        }

        @Expose
        @ConfigOption(name = "Others Boss", desc = "")
        @Accordion
        public OthersBoss othersBoss = new OthersBoss();
        public static class OthersBoss {

            @Expose
            @ConfigOption(name = "Highlight Others people boss", desc = "Highlight others players boss.\nYou need to hit them first.")
            @ConfigEditorBoolean
            public boolean highlight = true;

            @Expose
            @ConfigOption(name = "Highlight Color", desc = "What color to highlight the boss in.")
            @ConfigEditorColour
            public String highlightColor = "0:249:0:255:88";

            @Expose
            @ConfigOption(name = "Steak Alert", desc = "Show a title when you can steak the boss.")
            @ConfigEditorBoolean
            public boolean steakAlert = true;

            @Expose
            @ConfigOption(name = "Twinclaws warning", desc = "Send a title when Twinclaws is about to happen.")
            @ConfigEditorBoolean
            public boolean twinClawsTitle = true;
        }

        @Expose
        @ConfigOption(name = "Co-op Boss", desc = "")
        @Accordion
        public CoopBossHighlight coopsBossHighlight = new CoopBossHighlight();

        public static class CoopBossHighlight {
            @Expose
            @ConfigOption(name = "Highlight Co-op Boss", desc = "Highlight boss of your co-op member.")
            @ConfigEditorBoolean
            public boolean highlight = true;

            @Expose
            @ConfigOption(name = "Highlight Color", desc = "What color to highlight the boss in.")
            @ConfigEditorColour
            public String highlightColor = "0:249:0:255:88";

            @Expose
            @ConfigOption(name = "Co-op Members", desc = "Add your co-op member here.\n§eFormat: §7Name1,Name2,Name3")
            @ConfigEditorText
            public String coopMembers = "";

            @Expose
            @ConfigOption(name = "Steak Alert", desc = "Show a title when you can steak the boss.")
            @ConfigEditorBoolean
            public boolean steakAlert = true;

            @Expose
            @ConfigOption(name = "Twinclaws warning", desc = "Send a title when Twinclaws is about to happen.")
            @ConfigEditorBoolean
            public boolean twinClawsTitle = true;
        }

        @Expose
        @ConfigOption(name = "Transparency", desc = "Choose the transparency of the color.")
        @ConfigEditorSlider(minStep = 1, minValue = 1, maxValue = 250)
        public int withAlpha = 80;

        @Expose
        @ConfigOption(name = "See Through Blocs", desc = "Highlight even when behind others mobs/players")
        @ConfigEditorBoolean
        public boolean seeThrough = false;

        @Expose
        @ConfigOption(name = "Low Health", desc = "Change color when the boss is below 20% health.")
        @ConfigEditorBoolean
        public boolean changeColorWhenCanSteak = true;

        @Expose
        @ConfigOption(name = "Can use Steak Color", desc = "Color when the boss is below 20% health.")
        @ConfigEditorColour
        public String steakColor = "0:255:255:0:88";


        @Expose
        @ConfigOption(name = "Blood Ichor", desc = "")
        @Accordion
        public BloodIchor bloodIchor = new BloodIchor();

        public static class BloodIchor {
            @Expose
            @ConfigOption(name = "Highlight blood Ichor", desc = "Highlight the blood Ichor.")
            @ConfigEditorBoolean
            public boolean highlight = true;

            @Expose
            @ConfigOption(name = "Beacon Beam", desc = "Render a beacon beam where the Blood Ichor is.")
            @ConfigEditorBoolean
            public boolean renderBeam = true;

            @Expose
            @ConfigOption(name = "Color", desc = "Highlight color.")
            @ConfigEditorColour
            public String color = "0:199:100:0:88";
        }

        @Expose
        @ConfigOption(name = "Killer Spring", desc = "")
        @Accordion
        public KillerSpring killerSpring = new KillerSpring();

        public static class KillerSpring {
            @Expose
            @ConfigOption(name = "Highlight Killer Spring", desc = "Highlight the Killer Spring tower.")
            @ConfigEditorBoolean
            public boolean highlight = true;

            @Expose
            @ConfigOption(name = "Color", desc = "Highlight color.")
            @ConfigEditorColour
            public String color = "0:199:100:0:88";
        }
    }

    @ConfigOption(name = "Living Metal Suit Progress", desc = "")
    @Accordion
    public LivingMetalSuitProgress livingMetalSuitProgress = new LivingMetalSuitProgress();

    public static class LivingMetalSuitProgress {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Display progress Living Metal Suit")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Compact", desc = "Show a compacted version of the overlay when the set is maxed.")
        @ConfigEditorBoolean
        public boolean compactWhenMaxed = false;

        @Expose
        public Position position = new Position(100, 100);
    }

    @Expose
    @ConfigOption(name = "Show Motes Price", desc = "Show the Motes NPC price in the item lore.")
    @ConfigEditorBoolean
    public boolean showMotesPrice = true;
}
