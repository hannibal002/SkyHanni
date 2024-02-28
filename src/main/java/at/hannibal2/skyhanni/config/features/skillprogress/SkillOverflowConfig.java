package at.hannibal2.skyhanni.config.features.skillprogress;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;
import org.lwjgl.input.Keyboard;

public class SkillOverflowConfig {

    @Expose
    @ConfigOption(name = "Display", desc = "Enable the overflow calculation in the progress display.")
    @ConfigEditorBoolean
    public Property<Boolean> enableInDisplay = Property.of(false);

    @Expose
    @ConfigOption(name = "All Skill Display", desc = "Enable the overflow calculation in the all skill progress display.")
    @ConfigEditorBoolean
    public Property<Boolean> enableInAllDisplay = Property.of(false);

    @Expose
    @ConfigOption(name = "ETA Display", desc = "Enable the overflow calculation in the ETA skill display.")
    @ConfigEditorBoolean
    public Property<Boolean> enableInEtaDisplay = Property.of(false);

    @Expose
    @ConfigOption(name = "Progress Bar", desc = "Enable the overflow calculation in the progress bar of the display.")
    @ConfigEditorBoolean
    public Property<Boolean> enableInProgressBar = Property.of(false);

    @Expose
    @ConfigOption(name = "Skill Menu Stack Size", desc = "Enable the overflow calculation when the 'Skill Level' Item Number is enabled.")
    @ConfigEditorBoolean
    public boolean enableInSkillMenuAsStackSize = false;

    @Expose
    @ConfigOption(name = "Skill Menu Tooltips", desc = "Enable the overflow calculation in the tooltip of items in skills menu.")
    @ConfigEditorBoolean
    public boolean enableInSkillMenuTooltip = false;

    @Expose
    @ConfigOption(name = "Chat", desc = "Enable the overflow level up message when you gain an overflow level.")
    @ConfigEditorBoolean
    public boolean enableInChat = false;

    @Expose
    @ConfigOption(name = "Skill Avg" , desc = "Enable the overflow calculation for the skill average in SkyBlock and Your Skills menu.")
    @ConfigEditorBoolean
    public boolean enableSkillAvg = false;

    @Expose
    @ConfigOption(name = "Avg Key", desc = "Press this key in the SkyBlock/Your Skills menu while hovering the Avg. item to show more info.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int showAvgInfoKey = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Tooltip Button", desc = "Show a button in the Skill menu to toggle overflow in tooltip.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean tooltipButtonInSkillMenu = false;

    @Expose
    @ConfigOption(name = "Stack Size Button", desc = "Show a button in the Skill menu to toggle skill level overflow as stack size.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean stackSizeButtonInSkillMenu = false;
}
