package at.hannibal2.skyhanni.config.features.skillprogress;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorInfoText;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class SkillETADisplayConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show a display of your current active skill\n" +
        "with the XP/hour rate, ETA to the next level and current session time.")
    @ConfigEditorBoolean
    public Property<Boolean> enabled = Property.of(false);

    @Expose
    @ConfigOption(name = "Custom Goal", desc = "Choose a target level instead of showing progress to the next one.")
    @ConfigEditorBoolean
    public boolean enableCustomGoal = false;

    @Expose
    @ConfigOption(name = "Target Level", desc = "Desired target level.\n§eShould be §ahigher §ethan §ayour current level §eand §clower §ethan §c400.")
    @ConfigEditorText
    public String customGoalValue = "0";

    @Expose
    @ConfigOption(name = "Farming", desc = "After how much seconds the Farming session timer should pause.")
    @ConfigEditorSlider(minStep = 1, minValue = 3, maxValue = 60)
    public int farmingPauseTime = 3;

    @Expose
    @ConfigOption(name = "Mining", desc = "After how much seconds the Mining session timer should pause.")
    @ConfigEditorSlider(minStep = 1, minValue = 3, maxValue = 60)
    public int miningPauseTime = 3;

    @Expose
    @ConfigOption(name = "Combat", desc = "After how much seconds the Combat session timer should pause.")
    @ConfigEditorSlider(minStep = 1, minValue = 3, maxValue = 60)
    public int combatPauseTime = 30;

    @Expose
    @ConfigOption(name = "Foraging", desc = "After how much seconds the Foraging session timer should pause.")
    @ConfigEditorSlider(minStep = 1, minValue = 3, maxValue = 60)
    public int foragingPauseTime = 3;

    @Expose
    @ConfigOption(name = "Fishing", desc = "After how much seconds the Fishing session timer should pause.")
    @ConfigEditorSlider(minStep = 1, minValue = 3, maxValue = 60)
    public int fishingPauseTime = 15;
}
