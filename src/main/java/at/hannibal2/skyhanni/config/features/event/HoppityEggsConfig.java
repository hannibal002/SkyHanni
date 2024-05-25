package at.hannibal2.skyhanni.config.features.event;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class HoppityEggsConfig {

    @Expose
    @ConfigOption(name = "Hoppity Waypoints", desc = "Toggle guess waypoints for Hoppity's Hunt.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean waypoints = true;

    @Expose
    @ConfigOption(
        name = "Show Waypoints Immediately",
        desc = "Show a raw estimate waypoint immediately after clicking. " +
            "§cThis might cause issues with other particle sources."
    )
    @ConfigEditorBoolean
    public boolean waypointsImmediately = false;

    @Expose
    @ConfigOption(name = "Show Line", desc = "Show a line to the waypoint.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showLine = false;

    @Expose
    @ConfigOption(name = "Show All Waypoints", desc = "Show all possible egg waypoints for the current lobby. §e" +
        "Only works when you don't have an Egglocator in your inventory.")
    @ConfigEditorBoolean
    public boolean showAllWaypoints = false;

    @Expose
    @ConfigOption(name = "Show Unclaimed Eggs", desc = "Displays which eggs haven't been found in the last SkyBlock day.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showClaimedEggs = false;

    @Expose
    @ConfigOption(name = "Warn When Unclaimed", desc = "Warn when all three eggs are ready to be found.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean warnUnclaimedEggs = false;

    @Expose
    @ConfigOption(name = "Click to Warp", desc = "Makes the eggs ready chat message clickable to warp you to an island.")
    @ConfigEditorBoolean
    public boolean warpUnclaimedEggs = false;

    @Expose
    @ConfigOption(name = "Warp Destination", desc = "A custom island to warp to in the above option.")
    @ConfigEditorText
    public String warpDestination = "nucleus";

    @Expose
    @ConfigOption(name = "Show during Contest", desc = "Show during a farming contest.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showDuringContest = false;

    @Expose
    @ConfigOption(name = "Shared Hoppity Waypoints", desc = "Enable being able to share and receive egg waypoints in your lobby.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean sharedWaypoints = true;

    @Expose
    @ConfigOption(name = "Adjust player opacity", desc = "Adjust the opacity of players near shared & guessed egg waypoints. (in %)")
    @ConfigEditorSlider(minValue = 0, maxValue = 100, minStep = 1)
    public int playerOpacity = 40;

    @Expose
    @ConfigLink(owner = HoppityEggsConfig.class, field = "showClaimedEggs")
    public Position position = new Position(200, 120, false, true);

    @Expose
    @ConfigOption(name = "Highlight Hoppity Shop", desc = "Highlight items that haven't been bought from the Hoppity shop yet.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightHoppityShop = true;

    @Expose
    @ConfigOption(name = "Time in Chat", desc = "When the Egglocator can't find an egg, show the time until the next Hoppity event or egg spawn.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean timeInChat = true;

    @Expose
    @ConfigOption(name = "Compact Chat", desc = "Compact chat events when finding a Hoppity Egg.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean compactChat = false;
}
