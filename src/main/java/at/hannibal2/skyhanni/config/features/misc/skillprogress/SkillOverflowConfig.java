package at.hannibal2.skyhanni.config.features.misc.skillprogress;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

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
    @ConfigOption(name = "Progress Bar", desc = "Enable the overflow calculation in the progress bar of the display.")
    @ConfigEditorBoolean
    public Property<Boolean> enableInProgressBar = Property.of(false);

    @Expose
    @ConfigOption(name = "Skill Menu Stack Size", desc = "Enable the overflow calculation when the 'Skill Level' Item Number is enabled.")
    @ConfigEditorBoolean
    public boolean enableInSkillMenuAsStackSize = false;

    @Expose
    @ConfigOption(name = "Skill Menu Tooltips", desc = "Enable the overflow calculation in the tooltip of.")
    @ConfigEditorBoolean
    public boolean enableInSkillMenuTooltip = false;


}
