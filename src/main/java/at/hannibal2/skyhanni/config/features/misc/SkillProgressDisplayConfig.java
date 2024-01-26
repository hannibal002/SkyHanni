package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class SkillProgressDisplayConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the Skill Progress Display.")
    @ConfigEditorBoolean
    public Property<Boolean> enabled = Property.of(false);
    ;

    @Expose
    @ConfigOption(name = "Progress Bar", desc = "yes progress bar are cool.")
    @ConfigEditorBoolean
    public Property<Boolean> showProgressBar = Property.of(false);

    @Expose
    @ConfigOption(name = "Textured Bar", desc = "Use a textured progress bar.\n§eCan be changed with a resource pack.")
    @ConfigEditorBoolean
    public Property<Boolean> useTexturedBar = Property.of(false);

    @Expose
    @ConfigOption(name = "Chroma", desc = "Use the SBA like chroma effect on the bar.\n§eIf enabled, ignore the Bar Color setting.")
    @ConfigEditorBoolean
    public Property<Boolean> useChroma = Property.of(false);

    @Expose
    @ConfigOption(name = "Always Show", desc = "Always show the skill progress.")
    @ConfigEditorBoolean
    public Property<Boolean> alwaysShow = Property.of(false);

    @Expose
    @ConfigOption(name = "Bar Color", desc = "Color of the progress bar.")
    @ConfigEditorColour
    public String barStartColor = "0:255:255:0:0";

    @Expose
    @ConfigOption(name = "Show action left", desc = "Show action left until you reach the next level.")
    @ConfigEditorBoolean
    public Property<Boolean> showActionLeft = Property.of(false);

    @Expose
    @ConfigOption(name = "Use percentage", desc = "Use percentage instead of XP.")
    @ConfigEditorBoolean
    public Property<Boolean> usePercentage = Property.of(false);

    @Expose
    @ConfigOption(name = "Use Icon", desc = "Show the skill icon in the display.")
    @ConfigEditorBoolean
    public Property<Boolean> useIcon = Property.of(true);

    @Expose
    @ConfigOption(name = "Use Skill Name", desc = "Show the skill name in the display.")
    @ConfigEditorBoolean
    public Property<Boolean> useSkillName = Property.of(false);

    @Expose
    @ConfigOption(name = "Show Level", desc = "Show your current level in the display.")
    @ConfigEditorBoolean
    public Property<Boolean> showLevel = Property.of(true);

    @Expose
    @ConfigOption(name = "Show Overflow XP", desc = "Show overflow XP and level.\n§eOnly work when level 60.")
    @ConfigEditorBoolean
    public Property<Boolean> showOverflow = Property.of(true);

    @Expose
    public Position position = new Position(339, -100, false, true);

    @Expose
    public Position barPosition = new Position(339, -110, false, true);
}
