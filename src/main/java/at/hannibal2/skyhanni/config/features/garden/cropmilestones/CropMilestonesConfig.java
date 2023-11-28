package at.hannibal2.skyhanni.config.features.garden.cropmilestones;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.features.garden.cropmilestones.mushroompet.MushroomPetPerkConfig;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static at.hannibal2.skyhanni.config.features.garden.cropmilestones.MilestoneTextEntry.BLOCKS_PER_SECOND;
import static at.hannibal2.skyhanni.config.features.garden.cropmilestones.MilestoneTextEntry.CROPS_PER_MINUTE;
import static at.hannibal2.skyhanni.config.features.garden.cropmilestones.MilestoneTextEntry.MILESTONE_TIER;
import static at.hannibal2.skyhanni.config.features.garden.cropmilestones.MilestoneTextEntry.NUMBER_OUT_OF_TOTAL;
import static at.hannibal2.skyhanni.config.features.garden.cropmilestones.MilestoneTextEntry.TIME;
import static at.hannibal2.skyhanni.config.features.garden.cropmilestones.MilestoneTextEntry.TITLE;
import static at.hannibal2.skyhanni.config.features.garden.cropmilestones.TimeFormatEntry.YEAR;

public class CropMilestonesConfig {
    @Expose
    @ConfigOption(
        name = "Progress Display",
        desc = "Shows the progress and ETA until the next crop milestone is reached and the current crops/minute value. " +
            "§eRequires a tool with either a counter or Cultivating enchantment for full accuracy."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean progress = true;

    @Expose
    @ConfigOption(
        name = "Warn When Close",
        desc = "Warn with title and sound when the next crop milestone upgrade happens in 5 seconds. " +
            "Useful for switching to a different pet for leveling.")
    @ConfigEditorBoolean
    public boolean warnClose = false;

    @Expose
    @ConfigOption(
        name = "Time Format",
        desc = "Change the highest time unit to show (1h30m vs 90min)")
    @ConfigEditorDropdown()
    public Property<TimeFormatEntry> highestTimeFormat = Property.of(YEAR);

    @Expose
    @ConfigOption(
        name = "Maxed Milestone",
        desc = "Calculate the progress and ETA till maxed milestone (46) instead of next milestone.")
    @ConfigEditorBoolean
    public Property<Boolean> bestShowMaxedNeeded = Property.of(false);

    @Expose
    @ConfigOption(
        name = "Milestone Text",
        desc = "Drag text to change the appearance of the overlay.\n" +
            "Hold a farming tool to show the overlay."
    )
    @ConfigEditorDraggableList()
    public List<MilestoneTextEntry> text = new ArrayList<>(Arrays.asList(
        TITLE,
        MILESTONE_TIER,
        NUMBER_OUT_OF_TOTAL,
        TIME,
        CROPS_PER_MINUTE,
        BLOCKS_PER_SECOND
    ));

    @Expose
    @ConfigOption(name = "Block Broken Precision", desc = "The amount of decimals displayed in blocks/second.")
    @ConfigEditorSlider(
        minValue = 0,
        maxValue = 6,
        minStep = 1
    )
    public int blocksBrokenPrecision = 2;

    @Expose
    @ConfigOption(name = "Seconds Before Reset", desc = "How many seconds of not farming until blocks/second resets.")
    @ConfigEditorSlider(
        minValue = 2,
        maxValue = 60,
        minStep = 1
    )
    public int blocksBrokenResetTime = 5;

    @Expose
    public Position progressDisplayPos = new Position(-400, -200, false, true);

    @Expose
    @ConfigOption(name = "Best Crop", desc = "")
    @Accordion
    public NextConfig next = new NextConfig();

    @Expose
    @ConfigOption(name = "Mushroom Pet Perk", desc = "")
    @Accordion
    public MushroomPetPerkConfig mushroomPetPerk = new MushroomPetPerkConfig();

}
