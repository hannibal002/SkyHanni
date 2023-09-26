package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
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
        @FeatureToggle
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

    @ConfigOption(name = "Crux Talisman Progress", desc = "")
    @Accordion
    @Expose
    public CruxTalismanDisplay cruxTalisman = new CruxTalismanDisplay();

    public static class CruxTalismanDisplay {
        @Expose
        @ConfigOption(name = "Crux Talisman Display", desc = "Display progress of the Crux Talisman on screen.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Compact", desc = "Show a compacted version of the overlay when the talisman is maxed.")
        @ConfigEditorBoolean
        public boolean compactWhenMaxed = false;

        @Expose
        @ConfigOption(name = "Show Bonuses", desc = "Show bonuses you get from the talisman.")
        @ConfigEditorBoolean
        @FeatureToggle
        public Property<Boolean> showBonuses = Property.of(true);

        @Expose
        public Position position = new Position(144, 139, false, true);
    }

    @ConfigOption(name = "Enigma Soul Waypoints", desc = "")
    @Accordion
    @Expose
    public EnigmaSoulConfig enigmaSoulWaypoints = new EnigmaSoulConfig();

    public static class EnigmaSoulConfig {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Click on Enigma Souls in Rift Guides to highlight their location.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Color", desc = "Color of the Enigma Souls.")
        @ConfigEditorColour
        public String color = "0:120:13:49:255";

    }

    @ConfigOption(name = "Rift Areas", desc = "")
    @Accordion
    @Expose
    public RiftAreasConfig area = new RiftAreasConfig();

    public static class RiftAreasConfig {

        @ConfigOption(name = "Wyld Woods", desc = "")
        @Accordion
        @Expose
        public WyldWoodsConfig wyldWoodsConfig = new WyldWoodsConfig();

        public static class WyldWoodsConfig {

            @Expose
            @ConfigOption(name = "Shy Crux Warning", desc = "Shows a warning when a Shy Crux is going to steal your time. " +
                    "Useful if you play without volume.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean shyWarning = true;

            @ConfigOption(name = "Larvas", desc = "")
            @Accordion
            @Expose
            public LarvasConfig larvas = new LarvasConfig();

            public static class LarvasConfig {

                @Expose
                @ConfigOption(name = "Highlight", desc = "Highlight §cLarvas on trees §7while holding a §eLarva Hook §7in the hand.")
                @ConfigEditorBoolean
                @FeatureToggle
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
                @FeatureToggle
                public boolean highlight = true;

                @Expose
                @ConfigOption(name = "Color", desc = "Color of the Odonatas.")
                @ConfigEditorColour
                public String highlightColor = "0:120:13:49:255";

            }
        }

        @ConfigOption(name = "West Village", desc = "")
        @Accordion
        @Expose
        public WestVillageConfig westVillageConfig = new WestVillageConfig();

        public static class WestVillageConfig {

            @ConfigOption(name = "Kloon Hacking", desc = "")
            @Accordion
            @Expose
            public KloonHacking hacking = new KloonHacking();

            public static class KloonHacking {

                @Expose
                @ConfigOption(name = "Hacking Solver", desc = "Highlights the correct button to click in the hacking inventory.")
                @ConfigEditorBoolean
                @FeatureToggle
                public boolean solver = true;

                @Expose
                @ConfigOption(name = "Color Guide", desc = "Tells you which colour to pick.")
                @ConfigEditorBoolean
                @FeatureToggle
                public boolean colour = true;

                @Expose
                @ConfigOption(name = "Terminal Waypoints", desc = "While wearing the helmet, waypoints will appear at each terminal location.")
                @ConfigEditorBoolean
                @FeatureToggle
                public boolean waypoints = true;
            }
        }

        @Expose
        @ConfigOption(name = "Dreadfarm", desc = "")
        @Accordion
        public DreadfarmConfig dreadfarmConfig = new DreadfarmConfig();

        public static class DreadfarmConfig {
            @Expose
            @ConfigOption(name = "Agaricus Cap", desc = "Counts down the time until §eAgaricus Cap (Mushroom) " +
                    "§7changes color from brown to red and is breakable.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean agaricusCap = true;

            @ConfigOption(name = "Volt Crux", desc = "")
            @Accordion
            @Expose
            public VoltCruxConfig voltCrux = new VoltCruxConfig();

            public static class VoltCruxConfig {

                @Expose
                @ConfigOption(name = "Volt Warning", desc = "Shows a warning while a volt is discharging lightning.")
                @ConfigEditorBoolean
                @FeatureToggle
                public boolean voltWarning = true;

                @Expose
                @ConfigOption(name = "Volt Range Highlighter", desc = "Shows the area in which a Volt might strike lightning.")
                @ConfigEditorBoolean
                @FeatureToggle
                public boolean voltRange = true;

                @Expose
                @ConfigOption(name = "Volt Range Highlighter Color", desc = "In which color should the volt range be highlighted?")
                @ConfigEditorColour
                public String voltColour = "0:60:0:0:255";

                @Expose
                @ConfigOption(name = "Volt mood color", desc = "Change the color of the volt enemy depending on their mood.")
                @ConfigEditorBoolean
                @FeatureToggle
                public boolean voltMoodMeter = false;
            }

            @ConfigOption(name = "Wilted Berberis", desc = "")
            @Accordion
            @Expose
            public WiltedBerberisConfig wiltedBerberis = new WiltedBerberisConfig();

            public static class WiltedBerberisConfig {

                @Expose
                @ConfigOption(name = "Enabled", desc = "Show Wilted Berberis helper.")
                @ConfigEditorBoolean
                @FeatureToggle
                public boolean enabled = true;

                @Expose
                @ConfigOption(name = "Only on Farmland", desc = "Only show the helper while standing on Farmland blocks.")
                @ConfigEditorBoolean
                public boolean onlyOnFarmland = false;

                @Expose
                @ConfigOption(name = "Hide Particles", desc = "Hide the wilted berberis particles.")
                @ConfigEditorBoolean
                public boolean hideparticles = false;

            }
        }

        @ConfigOption(name = "Mirrorverse", desc = "")
        @Accordion
        @Expose
        public MirrorVerse mirrorVerseConfig = new MirrorVerse();

        public static class MirrorVerse {

            @ConfigOption(name = "Lava Maze", desc = "")
            @Accordion
            @Expose
            public LavaMazeConfig lavaMazeConfig = new LavaMazeConfig();

            public static class LavaMazeConfig {

                @Expose
                @ConfigOption(name = "Enabled", desc = "Helps solving the lava maze in the Mirrorverse by showing the correct way.")
                @ConfigEditorBoolean
                @FeatureToggle
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
                @FeatureToggle
                public boolean hidePlayers = false;
            }


            @ConfigOption(name = "Upside Down Parkour", desc = "")
            @Accordion
            @Expose
            public UpsideDownParkour upsideDownParkour = new UpsideDownParkour();

            public static class UpsideDownParkour {

                @Expose
                @ConfigOption(name = "Enabled", desc = "Helps solving the upside down parkour in the Mirrorverse by showing the correct way.")
                @ConfigEditorBoolean
                @FeatureToggle
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
                @FeatureToggle
                public boolean hidePlayers = false;
            }


            @ConfigOption(name = "Dance Room Helper", desc = "")
            @Accordion
            @Expose
            public DanceRoomHelper danceRoomHelper = new DanceRoomHelper();

            public static class DanceRoomHelper {

                @Expose
                @ConfigOption(name = "Enabled", desc = "Helps to solve the dance room in the Mirrorverse by showing multiple tasks at once.")
                @ConfigEditorBoolean
                @FeatureToggle
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
                @FeatureToggle
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
                @FeatureToggle
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
                @FeatureToggle
                public boolean hidePlayers = false;
            }
        }

//        @Expose
//        @ConfigOption(name = "Village Plaza", desc = "")
//        @Accordion
//        public VillagePlazaConfig villagePlazaConfig = new VillagePlazaConfig();
//
//        public static class VillagePlazaConfig {
//
//        }

        @Expose
        @ConfigOption(name = "Living Cave", desc = "")
        @Accordion
        public LivingCaveConfig livingCaveConfig = new LivingCaveConfig();

        public static class LivingCaveConfig {

            @Expose
            @ConfigOption(name = "Living Metal Suit Progress", desc = "")
            @Accordion
            public LivingMetalSuitProgress livingMetalSuitProgress = new LivingMetalSuitProgress();

            public static class LivingMetalSuitProgress {

                @Expose
                @ConfigOption(name = "Enabled", desc = "Display progress Living Metal Suit")
                @ConfigEditorBoolean
                @FeatureToggle
                public boolean enabled = false;

                @Expose
                @ConfigOption(name = "Compact", desc = "Show a compacted version of the overlay when the set is maxed.")
                @ConfigEditorBoolean
                public boolean compactWhenMaxed = false;

                @Expose
                public Position position = new Position(100, 100);
            }

            @Expose
            @ConfigOption(name = "Defense Blocks", desc = "")
            @Accordion
            public DefenseBlockConfig defenseBlockConfig = new DefenseBlockConfig();

            public static class DefenseBlockConfig {

                @Expose
                @ConfigOption(name = "Enabled", desc = "Show a line between Defense blocks and the mob and highlight the blocks.")
                @ConfigEditorBoolean
                @FeatureToggle
                public boolean enabled = true;

                @Expose
                @ConfigOption(name = "Hide Particles", desc = "Hide particles around Defense Blocks")
                @ConfigEditorBoolean
                @FeatureToggle
                public boolean hideParticles = false;

                @Expose
                @ConfigOption(name = "Color", desc = "Set the color of the lines, blocks and the entity.")
                @ConfigEditorColour
                public Property<String> color = Property.of("0:255:77:104:255");

            }

            @Expose
            @ConfigOption(name = "Living Metal Helper", desc = "")
            @Accordion
            public LivingCaveLivingMetalConfig livingCaveLivingMetalConfig = new LivingCaveLivingMetalConfig();

            public static class LivingCaveLivingMetalConfig {

                @Expose
                @ConfigOption(name = "Living Metal", desc = "Show a moving animation between Living Metal and the next block.")
                @ConfigEditorBoolean
                @FeatureToggle
                public boolean enabled = true;

                @Expose
                @ConfigOption(name = "Hide Particles", desc = "Hide Living Metal particles.")
                @ConfigEditorBoolean
                @FeatureToggle
                public boolean hideParticles = false;

            }
        }

        @Expose
        @ConfigOption(name = "Colosseum", desc = "")
        @Accordion
        public ColosseumConfig colosseumConfig = new ColosseumConfig();

        public static class ColosseumConfig {

            @Expose
            @ConfigOption(name = "Highlight Blobbercysts", desc = "Highlight Blobbercysts in Bacte fight.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean highlightBlobbercysts = true;
        }

        @Expose
        @ConfigOption(name = "Stillgore Chateau", desc = "")
        @Accordion
        public StillgoreChateauConfig stillgoreChateauConfig = new StillgoreChateauConfig();

        public static class StillgoreChateauConfig {

            @Expose
            @ConfigOption(name = "Blood Effigies", desc = "")
            @Accordion
            public EffigiesConfig bloodEffigies = new EffigiesConfig();

            public static class EffigiesConfig {

                @Expose
                @ConfigOption(name = "Enabled", desc = "Show locations of inactive Blood Effigy.")
                @ConfigEditorBoolean
                @FeatureToggle
                public boolean enabled = false;

                @Expose
                @ConfigOption(name = "Respawning Soon", desc = "Show effigies that are about to respawn.")
                @ConfigEditorBoolean
                @FeatureToggle
                public boolean respawningSoon = false;

                @Expose
                @ConfigOption(name = "Respawning Time", desc = "Time before effigies respawn to show.")
                @ConfigEditorSlider(
                        minValue = 1,
                        maxValue = 15,
                        minStep = 1
                )
                public int respwningSoonTime = 3;

                @Expose
                @ConfigOption(name = "Unknown Times", desc = "Show effigies without known time.")
                @ConfigEditorBoolean
                @FeatureToggle
                public boolean unknownTime = false;
            }
        }

//        @Expose
//        @ConfigOption(name = "Mountaintop", desc = "")
//        @Accordion
//        public MountaintopConfig mountaintopConfig = new MountaintopConfig();
//
//        public static class MountaintopConfig {
//
//        }

    }

    @Expose
    @ConfigOption(name = "Motes Sell Price", desc = "")
    @Accordion
    public Motes motes = new Motes();

    public static class Motes {

        @Expose
        @ConfigOption(name = "Show Motes Price", desc = "Show the Motes NPC price in the item lore.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean showPrice = true;

        @Expose
        @ConfigOption(name = "Burger Stacks", desc = "Set your McGrubber's burger stacks.")
        @ConfigEditorSlider(minStep = 1, minValue = 0, maxValue = 5)
        public int burgerStacks = 0;

        @Expose
        @ConfigOption(name = "Inventory value", desc = "")
        @Accordion
        public InventoryValue inventoryValue = new InventoryValue();

        public static class InventoryValue {
            @Expose
            @ConfigOption(name = "Inventory value", desc = "Show total Motes NPC price for the current opened inventory.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean enabled = true;

            @Expose
            @ConfigOption(name = "Number format type", desc = "Short: 1.2M\n" +
                    "Long: 1,200,000")
            @ConfigEditorDropdown(values = {"Short", "Long"})
            public int formatType = 0;

            @Expose
            public Position position = new Position(126, 156, false, true);
        }
    }

    @Expose
    @ConfigOption(name = "Motes Orbs", desc = "")
    @Accordion
    public MotesOrbsConfig motesOrbsConfig = new MotesOrbsConfig();

    public static class MotesOrbsConfig {

        @Expose
        @ConfigOption(name = "Highlight Motes Orbs", desc = "Highlight flying Motes Orbs.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Hide Particles", desc = "Hide normal motes orbs particles.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hideParticles = false;

    }

    @Expose
    @ConfigOption(name = "Highlight Guide", desc = "Highlight things to do in the Rift Guide.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightGuide = true;

    @Expose
    @ConfigOption(name = "Horsezooka Hider", desc = "Hide horses while holding the Horsezooka in the hand.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean horsezookaHider = false;
}
