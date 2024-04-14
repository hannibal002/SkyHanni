package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ChocolateFactoryConfig {

    @Expose
    @ConfigOption(name = "HoppityWaypoints", desc = "Toggle guess waypoints for Hoppity's Hunt.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean waypointsEnabled = true;

    @Expose
    @ConfigOption(name = "Show Stack Sizes", desc = "Shows addition info as many items in the chocolate menu as the stack size.")
    @ConfigEditorBoolean
    public boolean showStackSizes = true;

    @Expose
    @ConfigOption(name = "Highlight Upgrades", desc = "Highlight any upgrades that you can afford.")
    @ConfigEditorBoolean
    public boolean highlightUpgrades = true;

    @Expose
    @ConfigOption(name = "Use Middle Click", desc = "Click on slots with middle click to speed up interactions.")
    @ConfigEditorBoolean
    public boolean useMiddleClick = true;

    @Expose
    @ConfigOption(name = "Rabbit Warning", desc = "Warn when the rabbit that needs to be clicked appears.")
    @ConfigEditorBoolean
    public boolean rabbitWarning = true;

    @Expose
    @ConfigOption(name = "Show Claimed Eggs", desc = "Show which eggs have been found in the last SkyBlock day.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showClaimedEggs = false;

    @Expose
    @ConfigOption(
        name = "Barn Capacity Threshold",
        desc = "How close to your barn capacity threshold should you be warned about needing to upgrade it."
    )
    @ConfigEditorSlider(minValue = 3, maxValue = 20, minStep = 1)
    public int barnCapacityThreshold = 6;

    // todo remove probably or make false or something
    @Expose
    @ConfigOption(name = "Show All Waypoints", desc = "Show all possible egg waypoints for the current lobby.")
    @ConfigEditorBoolean
    public boolean showAllWaypoints = true;

    @Expose
    @ConfigLink(owner = ChocolateFactoryConfig.class, field = "showClaimedEggs")
    public Position position = new Position(33, 72, false, true);
}
