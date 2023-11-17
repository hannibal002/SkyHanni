package at.hannibal2.skyhanni.config.features.garden;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MoneyPerHourConfig {
    @Expose
    @ConfigOption(name = "Show Money per Hour",
        desc = "Displays the money per hour YOU get with YOUR crop/minute value when selling the item to bazaar. " +
            "Supports Bountiful, Mushroom Cow Perk, Armor Crops and Dicer Drops. Their toggles are below.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean display = true;

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(name = "Only Show Top", desc = "Only show the best # items.")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 25,
        minStep = 1
    )
    public int showOnlyBest = 5;

    @Expose
    @ConfigOption(name = "Extend Top List", desc = "Add current crop to the list if its lower ranked than the set limit by extending the list.")
    @ConfigEditorBoolean
    public boolean showCurrent = true;

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(
        name = "Always On",
        desc = "Always show the money/hour Display while on the garden.")
    @ConfigEditorBoolean
    public boolean alwaysOn = false;

    @Expose
    @ConfigOption(
        name = "Compact Mode",
        desc = "Hide the item name and the position number.")
    @ConfigEditorBoolean
    public boolean compact = false;

    @Expose
    @ConfigOption(
        name = "Compact Price",
        desc = "Show the price more compact.")
    @ConfigEditorBoolean
    public boolean compactPrice = false;

    @Expose
    @ConfigOption(
        name = "Use Custom",
        desc = "Use the custom format below instead of classic ➜ §eSell Offer §7and other profiles ➜ §eNPC Price.")
    @ConfigEditorBoolean
    public boolean useCustomFormat = false;

    @Expose
    @ConfigOption(
        name = "Custom Format",
        desc = "Set what prices to show")
    @ConfigEditorDraggableList(
        exampleText = {
            "§eSell Offer",
            "§eInstant Sell",
            "§eNPC Price"
        },
        requireNonEmpty = true
    )
    public List<Integer> customFormat = new ArrayList<>(Arrays.asList(0, 1, 2));

    @Expose
    @ConfigOption(
        name = "Merge Seeds",
        desc = "Merge the seeds price with the wheat price.")
    @ConfigEditorBoolean
    public boolean mergeSeeds = true;

    @Expose
    @ConfigOption(
        name = "Include Bountiful",
        desc = "Includes the coins from Bountiful in the calculation.")
    @ConfigEditorBoolean
    public boolean bountiful = true;

    @Expose
    @ConfigOption(
        name = "Include Mooshroom Cow",
        desc = "Includes the coins you get from selling the mushrooms from your Mooshroom Cow pet.")
    @ConfigEditorBoolean
    public boolean mooshroom = true;

    @Expose
    @ConfigOption(
        name = "Include Armor Drops",
        desc = "Includes the average coins/hr from your armor.")
    @ConfigEditorBoolean
    public boolean armor = true;

    @Expose
    @ConfigOption(
        name = "Include Dicer Drops",
        desc = "Includes the average coins/hr from your melon or pumpkin dicer.")
    @ConfigEditorBoolean
    public boolean dicer = true;

    @Expose
    @ConfigOption(
        name = "Hide Title",
        desc = "Hides the first line of 'Money Per Hour' entirely.")
    @ConfigEditorBoolean
    public boolean hideTitle = false;

    @Expose
    public Position pos = new Position(-330, 170, false, true);
}
