package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import io.github.moulberry.moulconfig.annotations.*;
import io.github.moulberry.moulconfig.observer.Property;

public class RiftConfig {

    @ConfigOption(name = "Rift Timer", desc = "")
    @Accordion
    public RiftTimerConfig timer = new RiftTimerConfig();

    public static class RiftTimerConfig {

        @ConfigOption(name = "Enabled", desc = "Show the remaining rift time, max time, percentage, and extra time changes.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @ConfigOption(name = "Max time", desc = "Show max time.")
        @ConfigEditorBoolean
        public boolean maxTime = true;

        @ConfigOption(name = "Percentage", desc = "Show percentage.")
        @ConfigEditorBoolean
        public boolean percentage = true;

        public Position timerPosition = new Position(10, 10, false, true);

    }

    @ConfigOption(name = "Crux Talisman Progress", desc = "")
    @Accordion
    public CruxTalisman cruxTalisman = new CruxTalisman();

    public static class CruxTalisman {
        @ConfigOption(name = "Crux Talisman Display", desc = "Display progress of the Crux Talisman on screen.")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @ConfigOption(name = "Compact", desc = "Show a compacted version of the overlay when the talisman is maxed.")
        @ConfigEditorBoolean
        public boolean compactWhenMaxed = false;

        @ConfigOption(name = "Show Bonuses", desc = "Show bonuses you get from the talisman.")
        @ConfigEditorBoolean
        public Property<Boolean> showBonuses = Property.of(true);

        public Position position = new Position(144, 139, false, true);
    }

    @ConfigOption(name = "Enigma Soul Waypoints", desc = "")
    @Accordion
    public EnigmaSoulConfig enigmaSoulWaypoints = new EnigmaSoulConfig();

    public static class EnigmaSoulConfig {

        @ConfigOption(name = "Enabled", desc = "Click on Enigma Souls in Rift Guides to highlight their location.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @ConfigOption(name = "Color", desc = "Color of the Enigma Souls.")
        @ConfigEditorColour
        public String color = "0:120:13:49:255";

    }

    @ConfigOption(name = "Rift Areas", desc = "")
    @Accordion
    public RiftAreasConfig area = new RiftAreasConfig();

    public static class RiftAreasConfig {

        @ConfigOption(name = "Wyld Woods", desc = "")
        @Accordion
        public WyldWoodsConfig wyldWoodsConfig = new WyldWoodsConfig();

        public static class WyldWoodsConfig {

            @ConfigOption(name = "Shy Crux Warning", desc = "Shows a warning when a Shy Crux is going to steal your time. " +
                    "Useful if you play without volume.")
            @ConfigEditorBoolean
            public boolean shyWarning = true;

            @ConfigOption(name = "Larvas", desc = "")
            @Accordion
            public LarvasConfig larvas = new LarvasConfig();

            public static class LarvasConfig {

                @ConfigOption(name = "Highlight", desc = "Highlight §cLarvas on trees §7while holding a §eLarva Hook §7in the hand.")
                @ConfigEditorBoolean
                public boolean highlight = true;

                @ConfigOption(name = "Color", desc = "Color of the Larvas.")
                @ConfigEditorColour
                public String highlightColor = "0:120:13:49:255";

            }

            @ConfigOption(name = "Odonatas", desc = "")
            @Accordion
            public OdonataConfig odonata = new OdonataConfig();

            public static class OdonataConfig {

                @ConfigOption(name = "Highlight", desc = "Highlight the small §cOdonatas §7flying around the trees while holding a " +
                        "§eEmpty Odonata Bottle §7in the hand.")
                @ConfigEditorBoolean
                public boolean highlight = true;

                @ConfigOption(name = "Color", desc = "Color of the Odonatas.")
                @ConfigEditorColour
                public String highlightColor = "0:120:13:49:255";

            }
        }

        @ConfigOption(name = "West Village", desc = "")
        @Accordion
        public WestVillageConfig westVillageConfig = new WestVillageConfig();

        public static class WestVillageConfig {

            @ConfigOption(name = "Kloon Hacking", desc = "")
            @Accordion
            public KloonHacking hacking = new KloonHacking();

            public static class KloonHacking {

                @ConfigOption(name = "Hacking Solver", desc = "Highlights the correct button to click in the hacking inventory.")
                @ConfigEditorBoolean
                public boolean solver = true;

                @ConfigOption(name = "Color Guide", desc = "Tells you which colour to pick.")
                @ConfigEditorBoolean
                public boolean colour = true;

                @ConfigOption(name = "Terminal Waypoints", desc = "While wearing the helmet, waypoints will appear at each terminal location.")
                @ConfigEditorBoolean
                public boolean waypoints = true;
            }
        }

        @ConfigOption(name = "Dreadfarm", desc = "")
        @Accordion
        public DreadfarmConfig dreadfarmConfig = new DreadfarmConfig();

        public static class DreadfarmConfig {
            @ConfigOption(name = "Agaricus Cap", desc = "Counts down the time until §eAgaricus Cap (Mushroom) " +
                    "§7changes color from brown to red and is breakable.")
            @ConfigEditorBoolean
            public boolean agaricusCap = true;

            @ConfigOption(name = "Volt Crux", desc = "")
            @Accordion
            public VoltCruxConfig voltCrux = new VoltCruxConfig();

            public static class VoltCruxConfig {

                @ConfigOption(name = "Volt Warning", desc = "Shows a warning while a volt is discharging lightning.")
                @ConfigEditorBoolean
                public boolean voltWarning = true;

                @ConfigOption(name = "Volt Range Highlighter", desc = "Shows the area in which a Volt might strike lightning.")
                @ConfigEditorBoolean
                public boolean voltRange = true;

                @ConfigOption(name = "Volt Range Highlighter Color", desc = "In which color should the volt range be highlighted?")
                @ConfigEditorColour
                public String voltColour = "0:60:0:0:255";

                @ConfigOption(name = "Volt mood color", desc = "Change the color of the volt enemy depending on their mood.")
                @ConfigEditorBoolean
                public boolean voltMoodMeter = false;
            }
        }

        @ConfigOption(name = "Mirror Verse", desc = "")
        @Accordion
        public MirrorVerse mirrorVerseConfig = new MirrorVerse();

        public static class MirrorVerse {

            @ConfigOption(name = "Lava Maze", desc = "")
            @Accordion
            public LavaMazeConfig lavaMazeConfig = new LavaMazeConfig();

            public static class LavaMazeConfig {

                @ConfigOption(name = "Enabled", desc = "Helps solving the lava maze in the mirror verse by showing the correct way.")
                @ConfigEditorBoolean
                public boolean enabled = true;

                @ConfigOption(name = "Look Ahead", desc = "Change how many platforms should be shown in front of you.")
                @ConfigEditorSlider(minStep = 1, maxValue = 30, minValue = 1)
                public Property<Integer> lookAhead = Property.of(3);

                @ConfigOption(name = "Rainbow Color", desc = "Show the rainbow color effect instead of a boring monochrome.")
                @ConfigEditorBoolean
                public Property<Boolean> rainbowColor = Property.of(true);

                @ConfigOption(name = "Monochrome Color", desc = "Set a boring monochrome color for the parkour platforms.")
                @ConfigEditorColour
                public Property<String> monochromeColor = Property.of("0:60:0:0:255");

                @ConfigOption(name = "Hide others players", desc = "Hide other players while doing the lava maze.")
                @ConfigEditorBoolean
                public boolean hidePlayers = false;
            }


            @ConfigOption(name = "Upside Down Parkour", desc = "")
            @Accordion
            public UpsideDownParkour upsideDownParkour = new UpsideDownParkour();

            public static class UpsideDownParkour {

                @ConfigOption(name = "Enabled", desc = "Helps solving the upside down parkour in the mirror verse by showing the correct way.")
                @ConfigEditorBoolean
                public boolean enabled = true;

                @ConfigOption(name = "Look Ahead", desc = "Change how many platforms should be shown in front of you.")
                @ConfigEditorSlider(minStep = 1, maxValue = 9, minValue = 1)
                public Property<Integer> lookAhead = Property.of(3);

                @ConfigOption(name = "Outline", desc = "Outlines the top edge of the platforms.")
                @ConfigEditorBoolean
                public boolean outline = true;

                @ConfigOption(name = "Rainbow Color", desc = "Show the rainbow color effect instead of a boring monochrome.")
                @ConfigEditorBoolean
                public Property<Boolean> rainbowColor = Property.of(true);

                @ConfigOption(name = "Monochrome Color", desc = "Set a boring monochrome color for the parkour platforms.")
                @ConfigEditorColour
                public Property<String> monochromeColor = Property.of("0:60:0:0:255");

                @ConfigOption(name = "Hide others players", desc = "Hide other players while doing the upside down parkour.")
                @ConfigEditorBoolean
                public boolean hidePlayers = false;
            }


            @ConfigOption(name = "Dance Room Helper", desc = "")
            @Accordion
            public DanceRoomHelper danceRoomHelper = new DanceRoomHelper();

            public static class DanceRoomHelper {

                @ConfigOption(name = "Enabled", desc = "Helps to solve the dance room in the mirror verse by showing multiple tasks at once.")
                @ConfigEditorBoolean
                public boolean enabled = false;

                @ConfigOption(name = "Lines to show", desc = "How many tasks you should see.")
                @ConfigEditorSlider(minStep = 1, maxValue = 49, minValue = 1)
                public int lineToShow = 3;

                @ConfigOption(name = "Space", desc = "Change the space between each line.")
                @ConfigEditorSlider(minStep = 1, maxValue = 10, minValue = -5)
                public int extraSpace = 0;

                @ConfigOption(name = "Hide others players", desc = "Hide other players inside the dance room.")
                @ConfigEditorBoolean
                public boolean hidePlayers = false;

                @ConfigOption(name = "Hide Title", desc = "Hide Instructions, \"§aIt's happening!\" §7and \"§aKeep it up!\" §7titles.")
                @ConfigEditorBoolean
                public boolean hideOriginalTitle = false;

                @ConfigOption(name = "Formatting", desc = "")
                @Accordion
                public DanceRoomFormatting danceRoomFormatting = new DanceRoomFormatting();

                public static class DanceRoomFormatting {

                    @ConfigOption(name = "Now", desc = "Formatting for \"Now:\"")
                    @ConfigEditorText
                    public String now = "&7Now:";

                    @ConfigOption(name = "Next", desc = "Formatting for \"Next:\"")
                    @ConfigEditorText
                    public String next = "&7Next:";

                    @ConfigOption(name = "Later", desc = "Formatting for \"Later:\"")
                    @ConfigEditorText
                    public String later = "&7Later:";

                    @ConfigOption(name = "Color Option", desc = "")
                    @Accordion
                    public Color color = new Color();

                    public static class Color {
                        @ConfigOption(name = "Move", desc = "Color for the Move instruction")
                        @ConfigEditorText
                        public String move = "&e";

                        @ConfigOption(name = "Stand", desc = "Color for the Stand instruction")
                        @ConfigEditorText
                        public String stand = "&e";

                        @ConfigOption(name = "Sneak", desc = "Color for the Sneak instruction")
                        @ConfigEditorText
                        public String sneak = "&5";

                        @ConfigOption(name = "Jump", desc = "Color for the Jump instruction")
                        @ConfigEditorText
                        public String jump = "&b";

                        @ConfigOption(name = "Punch", desc = "Color for the Punch instruction")
                        @ConfigEditorText
                        public String punch = "&d";

                        @ConfigOption(name = "Countdown", desc = "Color for the Countdown")
                        @ConfigEditorText
                        public String countdown = "&f";

                        @ConfigOption(name = "Default", desc = "Fallback color")
                        @ConfigEditorText
                        public String fallback = "&f";
                    }
                }

                public Position position = new Position(442, 239, false, true);
            }


            @ConfigOption(name = "Tubulator", desc = "")
            @Accordion
            public TubulatorConfig tubulatorConfig = new TubulatorConfig();

            public static class TubulatorConfig {

                @ConfigOption(name = "Enabled", desc = "Highlights the location of the invisible Tubulator blocks (Laser Parkour).")
                @ConfigEditorBoolean
                public boolean enabled = true;

                @ConfigOption(name = "Look Ahead", desc = "Change how many platforms should be shown in front of you.")
                @ConfigEditorSlider(minStep = 1, maxValue = 30, minValue = 1)
                public Property<Integer> lookAhead = Property.of(2);

                @ConfigOption(name = "Outline", desc = "Outlines the top edge of the platforms.")
                @ConfigEditorBoolean
                public boolean outline = true;

                @ConfigOption(name = "Rainbow Color", desc = "Show the rainbow color effect instead of a boring monochrome.")
                @ConfigEditorBoolean
                public Property<Boolean> rainbowColor = Property.of(true);

                @ConfigOption(name = "Monochrome Color", desc = "Set a boring monochrome color for the parkour platforms.")
                @ConfigEditorColour
                public Property<String> monochromeColor = Property.of("0:60:0:0:255");

                @ConfigOption(name = "Hide others players", desc = "Hide other players while doing the lava maze.")
                @ConfigEditorBoolean
                public boolean hidePlayers = false;
            }
        }

//        @ConfigOption(name = "Village Plaza", desc = "")
//        @Accordion
//        public VillagePlazaConfig villagePlazaConfig = new VillagePlazaConfig();
//
//        public static class VillagePlazaConfig {
//
//        }

        @ConfigOption(name = "Living Cave", desc = "")
        @Accordion
        public LivingCaveConfig livingCaveConfig = new LivingCaveConfig();

        public static class LivingCaveConfig {

            @ConfigOption(name = "Living Metal Suit Progress", desc = "")
            @Accordion
            public LivingMetalSuitProgress livingMetalSuitProgress = new LivingMetalSuitProgress();

            public static class LivingMetalSuitProgress {

                @ConfigOption(name = "Enabled", desc = "Display progress Living Metal Suit")
                @ConfigEditorBoolean
                public boolean enabled = false;

                @ConfigOption(name = "Compact", desc = "Show a compacted version of the overlay when the set is maxed.")
                @ConfigEditorBoolean
                public boolean compactWhenMaxed = false;

                public Position position = new Position(100, 100);
            }
        }

//        @ConfigOption(name = "Colosseum", desc = "")
//        @Accordion
//        public ColosseumConfig colosseumConfig = new ColosseumConfig();
//
//        public static class ColosseumConfig {
//
//        }

//        @ConfigOption(name = "Stillgore Chateau", desc = "")
//        @Accordion
//        public StillgoreChateauConfig stillgoreChateauConfig = new StillgoreChateauConfig();
//
//        public static class StillgoreChateauConfig {
//
//        }

//        @ConfigOption(name = "Mountaintop", desc = "")
//        @Accordion
//        public MountaintopConfig mountaintopConfig = new MountaintopConfig();
//
//        public static class MountaintopConfig {
//
//        }

    }

    @ConfigOption(name = "Highlight Guide", desc = "Highlight things to do in the Rift Guide.")
    @ConfigEditorBoolean
    public boolean highlightGuide = true;

    @ConfigOption(name = "Show Motes Price", desc = "Show the Motes NPC price in the item lore.")
    @ConfigEditorBoolean
    public boolean showMotesPrice = true;
}
