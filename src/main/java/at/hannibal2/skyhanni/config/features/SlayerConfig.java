package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigAccordionId;
import io.github.moulberry.moulconfig.annotations.ConfigEditorAccordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class SlayerConfig {

    @Expose
    @ConfigOption(name = "Enderman Slayer Features", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean enderman = false;

    @Expose
    @ConfigOption(name = "Yang Glyph (beacon)", desc = "")
    @ConfigAccordionId(id = 0)
    @Accordion
    public EndermanBeaconConfig endermanBeaconConfig = new EndermanBeaconConfig();

    public static class EndermanBeaconConfig {

        @Expose
        @ConfigOption(name = "Highlight Beacon",
            desc = "Highlight the Enderman Slayer Yang Glyph (beacon) in red color and added a timer for when he explodes. " +
                "Supports beacon in hand and beacon flying.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean highlightBeacon = true;

        @Expose
        @ConfigOption(name = "Beacon Color", desc = "Color of the beacon.")
        @ConfigEditorColour
        public String beaconColor = "0:255:255:0:88";

        @Expose
        @ConfigOption(name = "Show Warning", desc = "Displays a warning mid-screen when the Enderman Slayer throws a Yang Glyph (beacon).")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean showWarning = false;

        @Expose
        @ConfigOption(name = "Show Line", desc = "Draw a line starting at your crosshair to the beacon.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean showLine = false;

        @Expose
        @ConfigOption(name = "Line Color", desc = "Color of the line.")
        @ConfigEditorColour
        public String lneColor = "0:255:255:0:88";

        @Expose
        @ConfigOption(name = "Line Width", desc = "Width of the line.")
        @ConfigEditorSlider(minStep = 1, minValue = 1, maxValue = 10)
        public int lineWidth = 3;
    }

    @Expose
    @ConfigOption(name = "Highlight Nukekubi Skulls", desc = "Highlights the Enderman Slayer Nukekubi Skulls (Eyes).")
    @ConfigEditorBoolean
    @FeatureToggle
    @ConfigAccordionId(id = 0)
    public boolean endermanHighlightNukekebi = false;

    @Expose
    @ConfigOption(name = "Phase Display", desc = "Show the current phase of the Enderman Slayer in damage indcator.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean endermanPhaseDisplay = false;

    @Expose
    @ConfigOption(name = "Hide Particles", desc = "Hide particles around Enderman Slayer bosses and Mini-Bosses.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    @FeatureToggle
    public boolean endermanHideParticles = false;

    @Expose
    @ConfigOption(name = "Blaze", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean blaze = false;

    @Expose
    @ConfigOption(name = "Hellion Shields", desc = "")
    @ConfigEditorAccordion(id = 2)
    @ConfigAccordionId(id = 1)
    public boolean blazeHellion = false;

    @Expose
    @ConfigOption(name = "Colored Mobs", desc = "Color the Blaze Slayer boss and the demons in the right hellion shield color.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    @FeatureToggle
    public boolean blazeColoredMobs = false;

    @Expose
    @ConfigOption(name = "Blaze Daggers", desc = "Faster and permanent display for the Blaze Slayer daggers.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    @FeatureToggle
    public boolean blazeDaggers = false;

    @Expose
    @ConfigOption(name = "Right Dagger", desc = "Mark the right dagger to use for Blaze Slayer in the dagger overlay.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    @FeatureToggle
    public boolean blazeMarkRightHellionShield = false;

    @Expose
    @ConfigOption(name = "First Dagger", desc = "Select the first, left sided dagger for the display.")
    @ConfigEditorDropdown(values = {"Spirit/Crystal", "Ashen/Auric"})
    @ConfigAccordionId(id = 2)
    public int blazeFirstDagger = 0;

    @Expose
    @ConfigOption(name = "Hide Chat", desc = "Remove the wrong Blaze Slayer dagger messages from chat.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    @FeatureToggle
    public boolean blazeHideDaggerWarning = false;

    @Expose
    @ConfigOption(name = "Fire Pits", desc = "Warning when the fire pit phase starts for the Blaze Slayer tier 3 and 4.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    @FeatureToggle
    public boolean firePitsWarning = false;

    @Expose
    @ConfigOption(name = "Phase Display", desc = "Show the current phase of the Blaze Slayer boss.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean blazePhaseDisplay = false;

    @Expose
    @ConfigOption(name = "Clear View", desc = "Hide particles and fireballs near Blaze Slayer bosses and demons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    @FeatureToggle
    public boolean blazeClearView = false;

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
            @ConfigOption(name = "Highlight Your Boss", desc = "Highlight your own Vampire Slayer boss.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean highlight = true;

            @Expose
            @ConfigOption(name = "Highlight Color", desc = "What color to highlight the boss in.")
            @ConfigEditorColour
            public String highlightColor = "0:249:0:255:88";

            @Expose
            @ConfigOption(name = "Steak Alert", desc = "Show a title when you can steak your boss.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean steakAlert = true;

            @Expose
            @ConfigOption(name = "Twinclaws Title", desc = "Send a title when Twinclaws is about to happen.\nWork on others highlighted people boss.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean twinClawsTitle = true;

            @Expose
            @ConfigOption(name = "Twinclaws Sound", desc = "Play a sound when Twinclaws is about to happen.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean twinClawsSound = true;
        }

        @Expose
        @ConfigOption(name = "Others Boss", desc = "")
        @Accordion
        public OthersBoss othersBoss = new OthersBoss();

        public static class OthersBoss {

            @Expose
            @ConfigOption(name = "Highlight Others Boss", desc = "Highlight others players boss.\nYou need to hit them first.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean highlight = true;

            @Expose
            @ConfigOption(name = "Highlight Color", desc = "What color to highlight the boss in.")
            @ConfigEditorColour
            public String highlightColor = "0:249:0:255:88";

            @Expose
            @ConfigOption(name = "Steak Alert", desc = "Show a title when you can steak the boss.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean steakAlert = true;

            @Expose
            @ConfigOption(name = "Twinclaws Title", desc = "Send a title when Twinclaws is about to happen.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean twinClawsTitle = true;

            @Expose
            @ConfigOption(name = "Twinclaws Sound", desc = "Play a sound when Twinclaws is about to happen.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean twinClawsSound = true;
        }

        @Expose
        @ConfigOption(name = "Co-op Boss", desc = "")
        @Accordion
        public CoopBossHighlight coopBoss = new CoopBossHighlight();

        public static class CoopBossHighlight {
            @Expose
            @ConfigOption(name = "Highlight Co-op Boss", desc = "Highlight boss of your co-op member.")
            @ConfigEditorBoolean
            @FeatureToggle
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
            @FeatureToggle
            public boolean steakAlert = true;

            @Expose
            @ConfigOption(name = "Twinclaws Title", desc = "Send a title when Twinclaws is about to happen.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean twinClawsTitle = true;

            @Expose
            @ConfigOption(name = "Twinclaws Sound", desc = "Play a sound when Twinclaws is about to happen.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean twinClawsSound = true;
        }

        @Expose
        @ConfigOption(name = "Transparency", desc = "Choose the transparency of the color.")
        @ConfigEditorSlider(minStep = 1, minValue = 1, maxValue = 250)
        public int withAlpha = 80;

        @Expose
        @ConfigOption(name = "See Through Blocks", desc = "Highlight even when behind others mobs/players.")
        @ConfigEditorBoolean
        public boolean seeThrough = false;

        @Expose
        @ConfigOption(name = "Low Health", desc = "Change color when the boss is below 20% health.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean changeColorWhenCanSteak = true;

        @Expose
        @ConfigOption(name = "Can use Steak Color", desc = "Color when the boss is below 20% health.")
        @ConfigEditorColour
        public String steakColor = "0:255:255:0:88";

        @Expose
        @ConfigOption(name = "Twinclaws", desc = "Delay the sound and title of Twinclaws alert for a given amount in milliseconds.")
        @ConfigEditorSlider(minStep = 1, minValue = 0, maxValue = 1000)
        public int twinclawsDelay = 0;

        @Expose
        @ConfigOption(name = "Draw Line", desc = "Draw a line starting at your crosshair to the boss head.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean drawLine = false;

        @Expose
        @ConfigOption(name = "Line Color", desc = "Color of the line.")
        @ConfigEditorColour
        public String lineColor = "0:255:255:0:88";

        @Expose
        @ConfigOption(name = "Line Width", desc = "Width of the line.")
        @ConfigEditorSlider(minStep = 1, minValue = 1, maxValue = 10)
        public int lineWidth = 1;


        @Expose
        @ConfigOption(name = "Blood Ichor", desc = "")
        @Accordion
        public BloodIchor bloodIchor = new BloodIchor();

        public static class BloodIchor {
            @Expose
            @ConfigOption(name = "Highlight Blood Ichor", desc = "Highlight the Blood Ichor.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean highlight = false;

            @Expose
            @ConfigOption(name = "Beacon Beam", desc = "Render a beacon beam where the Blood Ichor is.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean renderBeam = true;

            @Expose
            @ConfigOption(name = "Color", desc = "Highlight color.")
            @ConfigEditorColour
            public String color = "0:199:100:0:88";

            @Expose
            @ConfigOption(name = "Show Lines", desc = "Draw lines that start from the head of the boss and end on the Blood Ichor.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean showLines = false;

            @Expose
            @ConfigOption(name = "Lines Start Color", desc = "Starting color of the lines.")
            @ConfigEditorColour
            public String linesColor = "0:255:255:13:0";

        }

        @Expose
        @ConfigOption(name = "Killer Spring", desc = "")
        @Accordion
        public KillerSpring killerSpring = new KillerSpring();

        public static class KillerSpring {
            @Expose
            @ConfigOption(name = "Highlight Killer Spring", desc = "Highlight the Killer Spring tower.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean highlight = false;

            @Expose
            @ConfigOption(name = "Color", desc = "Highlight color.")
            @ConfigEditorColour
            public String color = "0:199:100:0:88";

            @Expose
            @ConfigOption(name = "Show Lines", desc = "Draw lines that start from the head of the boss and end on the Killer Spring tower.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean showLines = false;

            @Expose
            @ConfigOption(name = "Lines Start Color", desc = "Starting color of the lines.")
            @ConfigEditorColour
            public String linesColor = "0:255:255:13:0";
        }
    }

    @Expose
    @ConfigOption(name = "Item Profit Tracker", desc = "")
    @Accordion
    public ItemProfitTracker itemProfitTracker = new ItemProfitTracker();

    public static class ItemProfitTracker {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Count all items you pick up while doing slayer, " +
            "keep track of how much you pay for starting slayers and calculating the overall profit.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = true;

        @Expose
        public Position pos = new Position(20, 20, false, true);

        @Expose
        @ConfigOption(name = "Price in Chat", desc = "Show an extra chat message when you pick up an item. " +
            "(This contains name, amount and price)")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean priceInChat = false;

        @Expose
        @ConfigOption(name = "Show Price From", desc = "Show price from Bazaar or NPC.")
        @ConfigEditorDropdown(values = {"Instant Sell", "Sell Offer", "NPC"})
        public int priceFrom = 1;

        @Expose
        @ConfigOption(name = "Minimum Price", desc = "Items below this price will not show up in chat.")
        @ConfigEditorSlider(minValue = 1, maxValue = 5_000_000, minStep = 1)
        public int minimumPrice = 100_000;

        @Expose
        @ConfigOption(name = "Title Warning", desc = "Show a title for expensive item pickups.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean titleWarning = false;

        @Expose
        @ConfigOption(name = "Title Price", desc = "Items above this price will show up as a title.")
        @ConfigEditorSlider(minValue = 1, maxValue = 20_000_000, minStep = 1)
        public int minimumPriceWarning = 500_000;
    }

    @Expose
    @ConfigOption(name = "Items on Ground", desc = "")
    @Accordion
    public ItemsOnGround itemsOnGround = new ItemsOnGround();

    public static class ItemsOnGround {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Show the name and price of items laying on the ground. §cOnly in slayer areas!")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Minimum Price", desc = "Items below this price will be ignored.")
        @ConfigEditorSlider(minValue = 1, maxValue = 1_000_000, minStep = 1)
        public int minimumPrice = 50_000;
    }

    @Expose
    @ConfigOption(name = "RNG Meter Display", desc = "")
    @Accordion
    public RngMeterDisplay rngMeterDisplay = new RngMeterDisplay();

    public static class RngMeterDisplay {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Display amount of bosses needed until next RNG meter drop.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Warn Empty", desc = "Warn when no item is set in the RNG Meter.")
        @ConfigEditorBoolean
        public boolean warnEmpty = false;

        @Expose
        @ConfigOption(name = "Hide Chat", desc = "Hide the RNG meter message from chat if current item is selected.")
        @ConfigEditorBoolean
        public boolean hideChat = true;

        @Expose
        public Position pos = new Position(410, 110, false, true);

    }

    @Expose
    @ConfigOption(name = "Boss Spawn Warning", desc = "")
    @Accordion
    public SlayerBossWarning slayerBossWarning = new SlayerBossWarning();

    public static class SlayerBossWarning {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Send a title when your boss is about to spawn.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Percent", desc = "The percentage at which the title and sound should be sent.")
        @ConfigEditorSlider(minStep = 1, minValue = 50, maxValue = 90)
        public int percent = 80;

        @Expose
        @ConfigOption(name = "Repeat", desc = "Resend the title and sound on every kill after reaching the configured percent value.")
        @ConfigEditorBoolean
        public boolean repeat = false;
    }

    @Expose
    @ConfigOption(name = "Miniboss Highlight", desc = "Highlight Slayer Mini-Boss in blue color.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean slayerMinibossHighlight = false;

    @Expose
    @ConfigOption(name = "Line to Miniboss", desc = "Adds a line to every Slayer Mini-Boss around you.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean slayerMinibossLine = false;

    @Expose
    @ConfigOption(name = "Hide Mob Names", desc = "Hide the name of the mobs you need to kill in order for the Slayer boss to spawn. Exclude mobs that are damaged, corrupted, runic or semi rare.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideMobNames = false;

    @Expose
    @ConfigOption(name = "Quest Warning", desc = "Warning when wrong Slayer quest is selected, or killing mobs for the wrong Slayer.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean questWarning = true;

    @Expose
    @ConfigOption(name = "Quest Warning Title", desc = "Sends a title when warning.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean questWarningTitle = true;
}
