package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ChocolateFactoryConfig {

    @Expose
    @ConfigOption(name = "Hoppity Egg ", desc = "")
    @Accordion
    public HoppityEggsConfig hoppityEggs = new HoppityEggsConfig();

    @Expose
    @ConfigOption(name = "Chocolate Factory Features", desc = "Global toggle for all Hoppity features.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

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
    @ConfigOption(
        name = "Barn Capacity Threshold",
        desc = "How close to your barn capacity threshold should you be warned about needing to upgrade it."
    )
    @ConfigEditorSlider(minValue = 3, maxValue = 20, minStep = 1)
    public int barnCapacityThreshold = 6;
}
