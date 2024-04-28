package at.hannibal2.skyhanni.config.features.event;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.features.event.chocolatefactory.ChocolateFactoryStats.ChocolateFactoryStat;
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
    @ConfigOption(name = "Hoppity Eggs", desc = "")
    @Accordion
    public HoppityEggsConfig hoppityEggs = new HoppityEggsConfig();

    @Expose
    @ConfigOption(name = "Chocolate Factory Features", desc = "Global toggle for all chocolate factory features.")
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
        desc = "Drag text to change what displays in the chocolate factory stats list and what order the text appears in."
    )
    @ConfigEditorDraggableList
    public List<ChocolateFactoryStat> statsDisplayList = new ArrayList<>(Arrays.asList(
        ChocolateFactoryStat.HEADER,
        ChocolateFactoryStat.CURRENT,
        ChocolateFactoryStat.THIS_PRESTIGE,
        ChocolateFactoryStat.ALL_TIME,
        ChocolateFactoryStat.EMPTY,
        ChocolateFactoryStat.PER_SECOND,
        ChocolateFactoryStat.PER_MINUTE,
        ChocolateFactoryStat.PER_HOUR,
        ChocolateFactoryStat.PER_DAY,
        ChocolateFactoryStat.EMPTY_2,
        ChocolateFactoryStat.MULTIPLIER,
        ChocolateFactoryStat.BARN,
        ChocolateFactoryStat.LEADERBOARD_POS
    ));

    @Expose
    @ConfigOption(name = "Show Stack Sizes", desc = "Shows additional info as many items in the chocolate menu as the stack size.")
    @ConfigEditorBoolean
    public boolean showStackSizes = true;

    @Expose
    @ConfigOption(name = "Highlight Upgrades", desc = "Highlight any upgrades that you can afford. Upgrade with star is the most optimal and the lightest color of green is the most optimal you can afford.")
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
    @ConfigEditorSlider(minValue = 0, maxValue = 20, minStep = 1)
    public int barnCapacityThreshold = 6;

    @Expose
    @ConfigOption(name = "Hoppity Collection Stats", desc = "Shows info about your hoppity rabbit collection.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hoppityCollectionStats = true;

    @Expose
    @ConfigLink(owner = ChocolateFactoryConfig.class, field = "statsDisplay")
    public Position position = new Position(183, 160, false, true);

    @Expose
    @ConfigLink(owner = ChocolateFactoryConfig.class, field = "hoppityCollectionStats")
    public Position hoppityStatsPosition = new Position(183, 160, false, true);
}
