package at.hannibal2.skyhanni.config.features.garden.pests;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.features.garden.GardenConfig;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class PestsConfig {

    @Expose
    @ConfigOption(name = "Pest Spawn", desc = "")
    @Accordion
    public PestSpawnConfig pestSpawn = new PestSpawnConfig();

    @Expose
    @ConfigOption(name = "Pest Finder", desc = "")
    @Accordion
    public PestFinderConfig pestFinder = new PestFinderConfig();

    @Expose
    @ConfigOption(name = "Pest Waypoint", desc = "")
    @Accordion
    public PestWaypointConfig pestWaypoint = new PestWaypointConfig();

    @Expose
    @ConfigOption(name = "Pest Timer", desc = "")
    @Accordion
    public PestTimerConfig pestTimer = new PestTimerConfig();

    @Expose
    @ConfigOption(name = "Pest Profit Tracker", desc = "")
    @Accordion
    public PestProfitTrackerConfig pestProfitTacker = new PestProfitTrackerConfig();

    @Expose
    @ConfigOption(name = "Spray", desc = "")
    @Accordion
    public SprayConfig spray = new SprayConfig();

    @ConfigOption(name = "Stereo Harmony", desc = "")
    @Accordion
    @Expose
    public StereoHarmonyConfig stereoHarmony = new StereoHarmonyConfig();

    @ConfigOption(name = "Pesthunter Profit Display", desc = "")
    @Accordion
    @Expose
    public PesthunterShopConfig pesthunterShop = new PesthunterShopConfig();

    @Expose
    @ConfigOption(
        name = "Bonus Chance Display",
        desc = "Displays your bonus pest chance and if it is enabled or not."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean pestChanceDisplay = false;

    @Expose
    @ConfigLink(owner = PestsConfig.class, field = "pestChanceDisplay")
    public Position pestChanceDisplayPosition = new Position(5, -115, false, true);
}

