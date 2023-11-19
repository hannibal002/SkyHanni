package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DiscordRPCConfig {

    @Expose
    @ConfigOption(name = "Enable Discord RPC", desc = "Details about your SkyBlock session displayed through Discord.")
    @ConfigEditorBoolean
    @FeatureToggle
    public Property<Boolean> enabled = Property.of(false);

    @Expose
    @ConfigOption(name = "First Line", desc = "Decide what to show in the first line.")
    @ConfigEditorDropdown(values = {
        "Nothing",
        "Location",
        "Purse",
        "Bits",
        "Stats",
        "Held Item",
        "SkyBlock Date",
        "Profile",
        "Slayer",
        "Custom",
        "Dynamic",
        "Crop Milestone",
        "Current Pet"
    })
    public Property<Integer> firstLine = Property.of(0);

    @Expose
    @ConfigOption(name = "Second Line", desc = "Decide what to show in the second line.")
    @ConfigEditorDropdown(values = {
        "Nothing",
        "Location",
        "Purse",
        "Bits",
        "Stats",
        "Held Item",
        "SkyBlock Date",
        "Profile",
        "Slayer",
        "Custom",
        "Dynamic",
        "Crop Milestone",
        "Current Pet"
    })
    public Property<Integer> secondLine = Property.of(0);

    @Expose
    @ConfigOption(name = "Custom", desc = "What should be displayed if you select \"Custom\" above.")
    @ConfigEditorText
    public Property<String> customText = Property.of("");

    @Expose
    @ConfigOption(name = "Dynamic Priority", desc = "Disable certain dynamic statuses, or change the priority in case two are triggered at the same time (higher up means higher priority).")
    @ConfigEditorDraggableList(
        exampleText = {
            "Crop Milestones",
            "Slayer",
            "Stacking Enchantment",
            "Dungeon",
            "AFK Indicator"
        }
    )
    public List<Integer> autoPriority = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));

    @Expose
    @ConfigOption(name = "Dynamic Fallback", desc = "What to show when none of your \"Dynamic Priority\" statuses are active.")
    @ConfigEditorDropdown(values = {
        "Nothing",
        "Location",
        "Purse",
        "Bits",
        "Stats",
        "Held Item",
        "SkyBlock Date",
        "Profile",
        "Slayer",
        "Custom",
        "Crop Milestone",
        "Current Pet"
    })
    public Property<Integer> auto = Property.of(0);
}
