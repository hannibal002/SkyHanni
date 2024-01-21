package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class SkillProgressDisplayConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the Skill Progress Display")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Progress Bar", desc = "yes progress bar are cool")
    @ConfigEditorBoolean
    public boolean showProgressBar = false;

    @Expose
    @ConfigOption(name = "Bar Color", desc = "Color of the progress bar")
    @ConfigEditorColour
    public String barColor = "0:255:255:0:0";

    @Expose
    @ConfigOption(name = "Show action left", desc = "Show action left until you reach the next level")
    @ConfigEditorBoolean
    public boolean showActionLeft = false;

    @Expose
    @ConfigOption(name = "Use percentage", desc = "Use percentage instead of XP")
    @ConfigEditorBoolean
    public boolean usePercentage = false;

    @Expose
    @ConfigOption(name = "Use Icon", desc = "Display skill icon in the display")
    @ConfigEditorBoolean
    public boolean useIcon = true;

    @Expose
    @ConfigOption(name = "Use Skill Name", desc = "Display the skill name in the display")
    @ConfigEditorBoolean
    public boolean useSkillName = false;

    @Expose
    @ConfigOption(name = "Show Level", desc = "Show your current level in the display")
    @ConfigEditorBoolean
    public boolean showLevel = true;

    @Expose
    @ConfigOption(name = "Show Overflow XP", desc = "Show overflow xp and level.\n§eOnly work when level 60")
    @ConfigEditorBoolean
    public boolean showOverflow = true;


    @Expose
    public Position position = new Position(339, -100, false, true);

    @Expose
    public Position barPosition = new Position(339, -110, false, true);
}
