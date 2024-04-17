package at.hannibal2.skyhanni.config.features.event;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.features.event.chocolatefactory.ChocolateFactoryStats.ChocolateFactoryStatsType;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChocolateFactoryConfig {

    @Expose
    @ConfigOption(name = "Hoppity Egg", desc = "")
    @Accordion
    public HoppityEggsConfig hoppityEggs = new HoppityEggsConfig();

    @Expose
    @ConfigOption(name = "Chocolate Factory Features", desc = "Global toggle for all Hoppity features.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Chocolate Factory Stats", desc = "Show general info about your chocolate factory.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean statsDisplay = true;

    @Expose
    @ConfigOption(
        name = "Stats List",
        desc = "Drag text to change what displays and which order it displays in."
    )
    @ConfigEditorDraggableList
    public List<ChocolateFactoryStatsType> statsDisplayList = new ArrayList<>(Arrays.asList(
        ChocolateFactoryStatsType.HEADER,
        ChocolateFactoryStatsType.CURRENT,
        ChocolateFactoryStatsType.THIS_PRESTIGE,
        ChocolateFactoryStatsType.ALL_TIME,
        ChocolateFactoryStatsType.EMPTY,
        ChocolateFactoryStatsType.PER_SECOND,
        ChocolateFactoryStatsType.PER_MINUTE,
        ChocolateFactoryStatsType.PER_HOUR,
        ChocolateFactoryStatsType.PER_DAY,
        ChocolateFactoryStatsType.EMPTY_2,
        ChocolateFactoryStatsType.MULTIPLIER,
        ChocolateFactoryStatsType.BARN,
        ChocolateFactoryStatsType.LEADERBOARD_POS
    ));

    @Expose
    @ConfigOption(name = "Show Stack Sizes", desc = "Shows additional info as many items in the chocolate menu as the stack size.")
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
        name = "Rabbit Crush Threshold",
        desc = "How close should you be to your barn capacity should you be before being warned about needing to upgrade it."
    )
    @ConfigEditorSlider(minValue = 3, maxValue = 20, minStep = 1)
    public int barnCapacityThreshold = 6;

    @Expose
    @ConfigLink(owner = ChocolateFactoryConfig.class, field = "statsDisplay")
    public Position position = new Position(183, 160, false, true);
}
