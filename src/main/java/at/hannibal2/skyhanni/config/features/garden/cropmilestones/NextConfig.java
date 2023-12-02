package at.hannibal2.skyhanni.config.features.garden.cropmilestones;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

// TODO moulconfig runnable support
public class NextConfig {
    @Expose
    @ConfigOption(
        name = "Best Display",
        desc = "Lists all crops and their ETA till next milestone. Sorts for best crop for getting garden or SkyBlock levels.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean bestDisplay = true;

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(name = "Sort Type", desc = "Sort the crops by either garden or SkyBlock EXP.")
    @ConfigEditorDropdown(values = {"Garden Exp", "SkyBlock Exp"})
    public int bestType = 0;

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(name = "Only Show Top", desc = "Only show the top # crops.")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 10,
        minStep = 1
    )
    public int showOnlyBest = 10;

    @Expose
    @ConfigOption(name = "Extend Top List", desc = "Add current crop to the list if its lower ranked than the set limit by extending the list.")
    @ConfigEditorBoolean
    public boolean showCurrent = true;

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(
        name = "Always On",
        desc = "Show the Best Display always while in the garden.")
    @ConfigEditorBoolean
    public boolean bestAlwaysOn = false;

    @Expose
    @ConfigOption(
        name = "Compact Display",
        desc = "A more compact best crop time: Removing the crop name and exp, hide the # number and using a more compact time format.")
    @ConfigEditorBoolean
    public boolean bestCompact = false;

    @Expose
    @ConfigOption(
        name = "Hide Title",
        desc = "Hides the 'Best Crop Time' line entirely.")
    @ConfigEditorBoolean
    public boolean bestHideTitle = false;

    @Expose
    public Position displayPos = new Position(-200, -200, false, true);
}
