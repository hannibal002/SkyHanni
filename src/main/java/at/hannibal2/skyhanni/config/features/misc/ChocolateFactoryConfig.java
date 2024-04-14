package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
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

}
