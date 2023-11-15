package at.hannibal2.skyhanni.config.features.combat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.features.combat.ghostcounter.GhostFormatting;
import at.hannibal2.skyhanni.features.combat.ghostcounter.GhostUtil;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorButton;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorInfoText;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CombatConfig {

    @Expose
    @ConfigOption(name = "Damage Indicator", desc = "")
    @Accordion
    public DamageIndicatorConfig damageIndicator = new DamageIndicatorConfig();

    public static class DamageIndicatorConfig {

        @Expose
        @ConfigOption(name = "Damage Indicator Enabled", desc = "Show the boss' remaining health.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Healing Chat Message", desc = "Sends a chat message when a boss heals themself.")
        @ConfigEditorBoolean
        public boolean healingMessage = false;

        @Expose
        @ConfigOption(
                name = "Boss Name",
                desc = "Change how the boss name should be displayed.")
        @ConfigEditorDropdown(values = {"Hidden", "Full Name", "Short Name"})
        public int bossName = 1;

        @Expose
        @ConfigOption(
                name = "Select Boss",
                desc = "Change what type of boss you want the damage indicator be enabled for."
        )
        @ConfigEditorDraggableList(
                exampleText = {
                        "§bDungeon All",
                        "§bNether Mini Bosses",
                        "§bVanquisher",
                        "§bEndstone Protector (not tested)",
                        "§bEnder Dragon (not finished)",
                        "§bRevenant Horror",
                        "§bTarantula Broodfather",
                        "§bSven Packmaster",
                        "§bVoidgloom Seraph",
                        "§bInferno Demonlord",
                        "§bHeadless Horseman (bugged)",
                        "§bDungeon Floor 1",
                        "§bDungeon Floor 2",
                        "§bDungeon Floor 3",
                        "§bDungeon Floor 4",
                        "§bDungeon Floor 5",
                        "§bDungeon Floor 6",
                        "§bDungeon Floor 7",
                        "§bDiana Mobs",
                        "§bSea Creatures",
                        "Dummy",
                        "§bArachne",
                        "§bThe Rift Bosses",
                        "§bRiftstalker Bloodfiend",
                        "§6Reindrake"
                }
        )
        //TODO only show currently working and tested features
        public List<Integer> bossesToShow = new ArrayList<>(Arrays.asList(0, 1, 2, 5, 6, 7, 8, 9, 18, 19, 21, 22, 23, 24));

        @Expose
        @ConfigOption(name = "Hide Damage Splash", desc = "Hiding damage splashes near the damage indicator.")
        @ConfigEditorBoolean
        public boolean hideDamageSplash = false;

        @Expose
        @ConfigOption(name = "Damage Over Time", desc = "Show damage and health over time below the damage indicator.")
        @ConfigEditorBoolean
        public boolean showDamageOverTime = false;

        @Expose
        @ConfigOption(name = "Hide Nametag", desc = "Hide the vanilla nametag of damage indicator bosses.")
        @ConfigEditorBoolean
        public boolean hideVanillaNametag = false;

        @Expose
        @ConfigOption(name = "Time to Kill", desc = "Show the time it takes to kill the slayer boss.")
        @ConfigEditorBoolean
        public boolean timeToKillSlayer = true;


        @Expose
        @ConfigOption(name = "Ender Slayer", desc = "")
        @Accordion
        public EnderSlayerConfig enderSlayer = new EnderSlayerConfig();

        public static class EnderSlayerConfig {

            @Expose
            @ConfigOption(name = "Laser Phase Timer", desc = "Show a timer when the laser phase will end.")
            @ConfigEditorBoolean
            public boolean laserPhaseTimer = false;

            @Expose
            @ConfigOption(name = "Health During Laser", desc = "Show the health of Voidgloom Seraph 4 during the laser phase.")
            @ConfigEditorBoolean
            public boolean showHealthDuringLaser = false;
        }

        @Expose
        @ConfigOption(name = "Vampire Slayer", desc = "")
        @Accordion
        public VampireSlayerConfig vampireSlayer = new VampireSlayerConfig();

        public static class VampireSlayerConfig {
            @Expose
            @ConfigOption(name = "HP Until Steak", desc = "Show the amount of HP missing until the Steak can be used on the Vampire Slayer on top of the boss.")
            @ConfigEditorBoolean
            public boolean hpTillSteak = false;

            @Expose
            @ConfigOption(name = "Mania Circles", desc = "Show a timer until the boss leaves the invincible Mania Circles state.")
            @ConfigEditorBoolean
            public boolean maniaCircles = false;

            @Expose
            @ConfigOption(name = "Percentage HP", desc = "Show the percentage of HP next to the HP.")
            @ConfigEditorBoolean
            public boolean percentage = false;
        }
    }

    @Expose
    @ConfigOption(name = "Ghost Counter", desc = "")
    @Accordion
    public GhostCounterConfig ghostCounter = new GhostCounterConfig();

    public static class GhostCounterConfig {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Enable the ghost counter (invisible creepers in the Dwarven Mines The Mist area).")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = true;

        @Expose
        @ConfigOption(
                name = "Display Text",
                desc = "Drag text to change the appearance of the overlay."
        )
        @ConfigEditorDraggableList(
                exampleText = {
                        "§6Ghosts Counter",
                        "  §bGhost Killed: 42",
                        "  §bSorrow: 6",
                        "  §bGhost since Sorrow: 1",
                        "  §bGhosts/Sorrow: 5",
                        "  §bVolta: 6",
                        "  §bPlasma: 8",
                        "  §bGhostly Boots: 1",
                        "  §bBag Of Cash: 4",
                        "  §bAvg Magic Find: 271",
                        "  §bScavenger Coins: 15,000",
                        "  §bKill Combo: 14",
                        "  §bHighest Kill Combo: 96",
                        "  §bSkill XP Gained: 145,648",
                        "  §bBestiary 1: 0/10",
                        "  §bXP/h: 810,410",
                        "  §bKills/h: 420",
                        "  §bETA: 14d",
                        "  §bMoney/h: 13,420,069",
                        "  §bMoney made: 14B"
                }
        )
        public List<Integer> ghostDisplayText = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 9, 10, 11, 12));

        @ConfigOption(name = "Text Formatting", desc = "")
        @Accordion
        @Expose
        public TextFormattingConfig textFormatting = new TextFormattingConfig();

        public static class TextFormattingConfig {

            @ConfigOption(name = "§eText Formatting Info", desc = "§e%session% §ris §e§lalways §rreplaced with\n" +
                    "§7the count for your current session.\n" +
                    "§7Reset when restarting the game.\n" +
                    "§7You can use §e&Z §7color code to use SBA chroma.")
            @ConfigEditorInfoText
            public boolean formatInfo = false;

            @ConfigOption(name = "Reset Formatting", desc = "Reset formatting to default text.")
            @ConfigEditorButton(buttonText = "Reset")
            public Runnable resetFormatting = GhostFormatting.INSTANCE::reset;

            @ConfigOption(name = "Export Formatting", desc = "Export current formatting to clipboard.")
            @ConfigEditorButton(buttonText = "Export")
            public Runnable exportFormatting = GhostFormatting.INSTANCE::export;

            @ConfigOption(name = "Import Formatting", desc = "Import formatting from clipboard.")
            @ConfigEditorButton(buttonText = "Import")
            public Runnable importFormatting = GhostFormatting.INSTANCE::importFormat;

            @Expose
            @ConfigOption(name = "Title", desc = "Title Line.")
            @ConfigEditorText
            public String titleFormat = "&6Ghost Counter";

            @Expose
            @ConfigOption(name = "Ghost Killed", desc = "Ghost Killed line.\n§e%value% §ris replaced with\n" +
                    "Ghost Killed.\n" +
                    "§e%session% §7is replaced with Ghost killed")
            @ConfigEditorText
            public String ghostKilledFormat = "  &6Ghost Killed: &b%value% &7(%session%)";

            @Expose
            @ConfigOption(name = "Sorrows", desc = "Sorrows drop line.\n" +
                    "§e%value% §7is replaced with\nsorrows dropped.")
            @ConfigEditorText
            public String sorrowsFormat = "  &6Sorrow: &b%value% &7(%session%)";

            @Expose
            @ConfigOption(name = "Ghost Since Sorrow", desc = "Ghost Since Sorrow line.\n" +
                    "§e%value% §7is replaced with\nGhost since last sorrow drop.")
            @ConfigEditorText
            public String ghostSinceSorrowFormat = "  &6Ghost since Sorrow: &b%value%";

            @Expose
            @ConfigOption(name = "Ghost Kill Per Sorrow", desc = "Ghost Kill Per Sorrow line.\n" +
                    "§e%value% §7is replaced with\naverage ghost kill per sorrow drop.")
            @ConfigEditorText
            public String ghostKillPerSorrowFormat = "  &6Ghosts/Sorrow: &b%value%";

            @Expose
            @ConfigOption(name = "Voltas", desc = "Voltas drop line.\n" +
                    "§e%value% §7is replaced with\nvoltas dropped.")
            @ConfigEditorText
            public String voltasFormat = "  &6Voltas: &b%value% &7(%session%)";

            @Expose
            @ConfigOption(name = "Plasmas", desc = "Plasmas drop line.\n" +
                    "§e%value% §7is replaced with\nplasmas dropped.")
            @ConfigEditorText
            public String plasmasFormat = "  &6Plasmas: &b%value% &7(%session%)";

            @Expose
            @ConfigOption(name = "Ghostly Boots", desc = "Ghostly Boots drop line.\n" +
                    "§e%value% §7is replaced with\nGhostly Boots dropped.")
            @ConfigEditorText
            public String ghostlyBootsFormat = "  &6Ghostly Boots: &b%value% &7(%session%)";

            @Expose
            @ConfigOption(name = "Bag Of Cash", desc = "Bag Of Cash drop line.\n" +
                    "§e%value% §7is replaced with\nBag Of Cash dropped.")
            @ConfigEditorText
            public String bagOfCashFormat = "  &6Bag Of Cash: &b%value% &7(%session%)";

            @Expose
            @ConfigOption(name = "Average Magic Find", desc = "Average Magic Find line.\n" +
                    "§e%value% §7is replaced with\nAverage Magic Find.")
            @ConfigEditorText
            public String avgMagicFindFormat = "  &6Avg Magic Find: &b%value%";

            @Expose
            @ConfigOption(name = "Scavenger Coins", desc = "Scavenger Coins line.\n" +
                    "§e%value% §7is replaced with\nCoins earned from kill ghosts.\nInclude: Scavenger Enchant, Scavenger Talismans, Kill Combo.")
            @ConfigEditorText
            public String scavengerCoinsFormat = "  &6Scavenger Coins: &b%value% &7(%session%)";

            @Expose
            @ConfigOption(name = "Kill Combo", desc = "Kill Combo line.\n" +
                    "§e%value% §7is replaced with\nYour current kill combo.")
            @ConfigEditorText
            public String killComboFormat = "  &6Kill Combo: &b%value%";

            @Expose
            @ConfigOption(name = "Highest Kill Combo", desc = "Highest Kill Combo line.\n" +
                    "§e%value% §7is replaced with\nYour current highest kill combo.")
            @ConfigEditorText
            public String highestKillComboFormat = "  &6Highest Kill Combo: &b%value% &7(%session%)";

            @Expose
            @ConfigOption(name = "Skill XP Gained", desc = "Skill XP Gained line.\n" +
                    "§e%value% §7is replaced with\nSkill XP Gained from killing Ghosts.")
            @ConfigEditorText
            public String skillXPGainFormat = "  &6Skill XP Gained: &b%value% &7(%session%)";

            @ConfigOption(name = "Bestiary Formatting", desc = "")
            @Accordion
            @Expose
            public BestiaryFormattingConfig bestiaryFormatting = new BestiaryFormattingConfig();

            public static class BestiaryFormattingConfig {

                @Expose
                @ConfigOption(name = "Bestiary", desc = "Bestiary Progress line.\n§e%value% §7is replaced with\n" +
                        "Your current progress to next level.\n" +
                        "§e%currentLevel% &7is replaced with your current bestiary level\n" +
                        "§e%nextLevel% §7is replaced with your current bestiary level +1.\n" +
                        "§e%value% §7is replaced with one of the text below.")
                @ConfigEditorText
                public String base = "  &6Bestiary %display%: &b%value%";

                @Expose
                @ConfigOption(name = "No Data", desc = "Text to show when you need to open the\nBestiary Menu to gather data.")
                @ConfigEditorText
                public String openMenu = "§cOpen Bestiary Menu !";

                @Expose
                @ConfigOption(name = "Maxed", desc = "Text to show when your bestiary for ghost is at max level.\n" +
                        "§e%currentKill% §7is replaced with your current total kill.")
                @ConfigEditorText
                public String maxed = "%currentKill% (&c&lMaxed!)";

                @Expose
                @ConfigOption(name = "Progress to Max", desc = "Text to show progress when the §eMaxed Bestiary §7option is §aON\n" +
                        "§e%currentKill% §7is replaced with your current total kill.")
                @ConfigEditorText
                public String showMax_progress = "%currentKill%/250k (%percentNumber%%)";

                @Expose
                @ConfigOption(name = "Progress", desc = "Text to show progress when the §eMaxed Bestiary§7 option is §cOFF\n" +
                        "§e%currentKill% §7is replaced with how many kill you have to the next level.\n" +
                        "§e%killNeeded% §7is replaced with how many kill you need to reach the next level.")
                @ConfigEditorText
                public String progress = "%currentKill%/%killNeeded%";
            }


            @ConfigOption(name = "XP Per Hour Formatting", desc = "")
            @Accordion
            @Expose
            public XPHourFormattingConfig xpHourFormatting = new XPHourFormattingConfig();

            public static class XPHourFormattingConfig {

                @Expose
                @ConfigOption(name = "XP/h", desc = "XP Per Hour line.\n" +
                        "§e%value% §7is replaced with one of the text below.")
                @ConfigEditorText
                public String base = "  &6XP/h: &b%value%";

                @Expose
                @ConfigOption(name = "No Data", desc = "XP Per Hour line.\n§e%value% §7is replaced with\nEstimated amount of combat xp you gain per hour.")
                @ConfigEditorText
                public String noData = "&bN/A";

                @Expose
                @ConfigOption(name = "Paused", desc = "Text displayed next to the time \n" +
                        "when you are doing nothing for a given amount of seconds")
                @ConfigEditorText
                public String paused = "&c(PAUSED)";
            }


            @ConfigOption(name = "ETA Formatting", desc = "")
            @Accordion
            @Expose
            public ETAFormattingConfig etaFormatting = new ETAFormattingConfig();

            public static class ETAFormattingConfig {
                @Expose
                @ConfigOption(name = "ETA to next level", desc = "ETA To Next Level Line.\n" +
                        "§e%value% §7is replaced with one of the text below.")
                @ConfigEditorText
                public String base = "  &6ETA: &b%value%";

                @Expose
                @ConfigOption(name = "Maxed!", desc = "So you really maxed ghost bestiary ?")
                @ConfigEditorText
                public String maxed = "&c&lMAXED!";

                @Expose
                @ConfigOption(name = "No Data", desc = "Start killing some ghosts !")
                @ConfigEditorText
                public String noData = "&bN/A";

                @Expose
                @ConfigOption(name = "Progress", desc = "Text to show progress to next level.")
                @ConfigEditorText
                public String progress = "&b%value%";

                @Expose
                @ConfigOption(name = "Paused", desc = "Text displayed next to the time \n" +
                        "when you are doing nothing for a given amount of seconds")
                @ConfigEditorText
                public String paused = "&c(PAUSED)";

                @Expose
                @ConfigOption(name = "Time", desc = "§e%days% §7is replaced with days remaining.\n" +
                        "§e%hours% §7is replaced with hours remaining.\n" +
                        "§e%minutes% §7is replaced with minutes remaining.\n" +
                        "§e%seconds% §7is replaced with seconds remaining.")
                @ConfigEditorText
                public String time = "&6%days%%hours%%minutes%%seconds%";
            }

            @ConfigOption(name = "Kill Per Hour Formatting", desc = "")
            @Expose
            @Accordion
            public KillHourFormattingConfig killHourFormatting = new KillHourFormattingConfig();

            public static class KillHourFormattingConfig {
                @Expose
                @ConfigOption(name = "Kill/h", desc = "Kill Per Hour line.\n§e%value% §7is replaced with\nEstimated kills per hour you get.")
                @ConfigEditorText
                public String base = "  &6Kill/h: &b%value%";

                @Expose
                @ConfigOption(name = "No Data", desc = "Start killing some ghosts !")
                @ConfigEditorText
                public String noData = "&bN/A";

                @Expose
                @ConfigOption(name = "Paused", desc = "Text displayed next to the time \n" +
                        "when you are doing nothing for a given amount of seconds")
                @ConfigEditorText
                public String paused = "&c(PAUSED)";
            }


            @Expose
            @ConfigOption(name = "Money Per Hour", desc = "Money Per Hour.\n§e%value% §7is replaced with\nEstimated money you get per hour\n" +
                    "Calculated with your kill per hour and your average magic find.")
            @ConfigEditorText
            public String moneyHourFormat = "  &6$/h: &b%value%";

            @Expose
            @ConfigOption(name = "Money made", desc = "Calculate the money you made.\nInclude §eSorrow§7, §ePlasma§7, §eVolta§7, §e1M coins drop\n" +
                    "§eGhostly Boots§7, §eScavenger coins.\n" +
                    "§cUsing current Sell Offer value.")
            @ConfigEditorText
            public String moneyMadeFormat = "  &6Money made: &b%value%";
        }

        @Expose
        @ConfigOption(name = "Extra space", desc = "Space between each line of text.")
        @ConfigEditorSlider(
                minValue = -5,
                maxValue = 10,
                minStep = 1)
        public int extraSpace = 1;

        @Expose
        @ConfigOption(name = "Pause Timer", desc = "How many seconds does it wait before pausing.")
        @ConfigEditorSlider(
                minValue = 1,
                maxValue = 20,
                minStep = 1
        )
        public int pauseTimer = 3;

        @Expose
        @ConfigOption(name = "Show only in The Mist", desc = "Show the overlay only when you are in The Mist.")
        @ConfigEditorBoolean
        public boolean onlyOnMist = true;

        @Expose
        @ConfigOption(name = "Maxed Bestiary", desc = "Show progress to max bestiary instead of next level.")
        @ConfigEditorBoolean
        public boolean showMax = false;

        @ConfigOption(name = "Reset", desc = "Reset the counter.")
        @ConfigEditorButton(buttonText = "Reset")
        public Runnable resetCounter = GhostUtil.INSTANCE::reset;

        @Expose
        public Position position = new Position(50, 50, false, true);
    }

    @Expose
    @ConfigOption(name = "Summonings", desc = "")
    @Accordion
    public SummoningsConfig summonings = new SummoningsConfig();

    public static class SummoningsConfig {

        @Expose
        @ConfigOption(name = "Summoning Soul Display", desc = "Show the name of dropped Summoning Souls laying on the ground. " +
                "§cNot working in Dungeons if Skytils' 'Hide Non-Starred Mobs Nametags' feature is enabled!")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean summoningSoulDisplay = false;

        @Expose
        @ConfigOption(name = "Summoning Mob Display", desc = "Show the health of your spawned summons.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean summoningMobDisplay = false;

        @Expose
        public Position summoningMobDisplayPos = new Position(10, 10, false, true);

        @Expose
        @ConfigOption(name = "Summoning Mob Nametag", desc = "Hide the nametag of your spawned summons.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean summoningMobHideNametag = false;

        @Expose
        @ConfigOption(name = "Summoning Mob Color", desc = "Marks own summons green.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean summoningMobColored = false;
    }

    @Expose
    @ConfigOption(name = "Mobs", desc = "")
    @Accordion
    public MobsConfig mobs = new MobsConfig();

    public static class MobsConfig {

        @Expose
        @ConfigOption(name = "Highlighters", desc = "")
        public boolean highlighters = false;

        @Expose
        @ConfigOption(name = "Area Boss", desc = "Highlight Golden Ghoul, Old Wolf, Voidling Extremist and Millenia-Aged Blaze.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean areaBossHighlight = true;

        @Expose
        @ConfigOption(name = "Arachne Keeper", desc = "Highlight the Arachne Keeper in the Spider's Den in purple color.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean arachneKeeperHighlight = true;

        @Expose
        @ConfigOption(name = "Corleone", desc = "Highlight Boss Corleone in the Crystal Hollows.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean corleoneHighlighter = true;

        @Expose
        @ConfigOption(name = "Zealot", desc = "Highlight Zealots and Bruisers in The End.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean zealotBruiserHighlighter = false;

        @Expose
        @ConfigOption(
                name = "Special Zealots",
                desc = "Highlight Special Zealots (the ones that drop Summoning Eyes) in the End."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean specialZealotHighlighter = true;

        @Expose
        @ConfigOption(name = "Corrupted Mob", desc = "Highlight corrupted mobs in purple color.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean corruptedMobHighlight = false;

        @Expose
        @ConfigOption(name = "Arachne Boss", desc = "Highlight the Arachne boss in red and mini-bosses in orange.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean arachneBossHighlighter = true;

        @Expose
        @ConfigOption(name = "Respawn Timers", desc = "")
        public boolean timers = false;

        @Expose
        @ConfigOption(
                name = "Area Boss",
                desc = "Show a timer when Golden Ghoul, Old Wolf, Voidling Extremist or Millenia-Aged Blaze respawns. " +
                        "§cSometimes it takes 20-30 seconds to calibrate correctly."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean areaBossRespawnTimer = false;

        @Expose
        @ConfigOption(
                name = "Arachne Spawn Timer",
                desc = "Show a timer when Arachne fragments or crystals are placed to indicate how long " +
                        "until the boss will spawn. §cTimer may be 1-2 seconds off."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean showArachneSpawnTimer = true;

        @Expose
        @ConfigOption(name = "Enderman TP Hider", desc = "Stops the Enderman Teleportation animation.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean endermanTeleportationHider = true;

        @Expose
        @ConfigOption(name = "Arachne Minis Hider", desc = "Hides the nametag above Arachne minis.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hideNameTagArachneMinis = true;
    }

    @Expose
    @ConfigOption(name = "Bestiary", desc = "")
    @Accordion
    public BestiaryConfig bestiary = new BestiaryConfig();

    public static class BestiaryConfig {
        @Expose
        @ConfigOption(name = "Enable", desc = "Show Bestiary Data overlay in the Bestiary menu.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Number format", desc = "Short: 1.1k\nLong: 1.100")
        @ConfigEditorDropdown(values = {"Short", "Long"})
        public int numberFormat = 0;

        @Expose
        @ConfigOption(name = "Display type", desc = "Choose what the display should show")
        @ConfigEditorDropdown(values = {
                "Global to max",
                "Global to next tier",
                "Lowest total kills",
                "Highest total kills",
                "Lowest kills needed to max",
                "Highest kills needed to max",
                "Lowest kills needed to next tier",
                "Highest kills needed to next tier"
        })
        public int displayType = 0;

        @Expose
        @ConfigOption(name = "Hide maxed", desc = "Hide maxed mobs.")
        @ConfigEditorBoolean
        public boolean hideMaxed = false;

        @Expose
        @ConfigOption(name = "Replace Romans", desc = "Replace Roman numerals (IX) with regular numbers (9)")
        @ConfigEditorBoolean
        public boolean replaceRoman = false;

        @Expose
        public Position position = new Position(100, 100, false, true);
    }

    @Expose
    @ConfigOption(name = "Ender Node Tracker", desc = "")
    @Accordion
    public EnderNodeConfig enderNodeTracker = new EnderNodeConfig();

    public static class EnderNodeConfig {
        @Expose
        @ConfigOption(
                name = "Enabled",
                desc = "Tracks all of your drops from mining Ender Nodes in the End.\n" +
                        "Also tracks drops from Endermen."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @Expose
        @ConfigOption(
                name = "Text Format",
                desc = "Drag text to change the appearance of the overlay."
        )
        @ConfigEditorDraggableList(
                exampleText = {
                        "§5§lEnder Node Tracker",
                        "§d1,303 Ender Nodes Mined",
                        "§615.3M Coins Made",
                        " ",
                        "§b123 §cEndermite Nest",
                        "§b832 §aEnchanted End Stone",
                        "§b230 §aEnchanted Obsidian",
                        "§b1630 §aEnchanted Ender Pearl",
                        "§b85 §aGrand Experience Bottle",
                        "§b4 §9Titanic Experience Bottle",
                        "§b15 §9End Stone Shulker",
                        "§b53 §9End Stone Geode",
                        "§b10 §d◆ Magical Rune I",
                        "§b24 §5Ender Gauntlet",
                        "§b357 §5Mite Gel",
                        "§b2 §cShrimp The Fish",
                        " ",
                        "§b200 §5Ender Armor",
                        "§b24 §5Ender Helmet",
                        "§b24 §5Ender Chestplate",
                        "§b24 §5Ender Leggings",
                        "§b24 §5Ender Boots",
                        "§b24 §5Ender Necklace",
                        "§f10§7-§a8§7-§93§7-§52§7-§61 §fEnderman Pet",
                        " "
                }
        )
        public Property<List<Integer>> textFormat = Property.of(new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 12, 14, 15, 16, 17, 23)));

        @Expose
        public Position position = new Position(10, 80, false, true);
    }

    @Expose
    @ConfigOption(name = "Hide Damage Splash", desc = "Hide all damage splashes anywhere in SkyBlock.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideDamageSplash = false;
}
