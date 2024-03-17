package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class QuiverDisplayConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Show the number of arrows you have.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    public Position quiverDisplayPos = new Position(260, 80);

    @Expose
    @ConfigOption(name = "Show arrow icon", desc = "")
    @ConfigEditorBoolean
    public boolean showIcon = true;

    @Expose
    @ConfigOption(name = "Show only with bow", desc = "Only show when a bow is in your inventory.")
    @ConfigEditorBoolean
    public boolean onlyWithBow = true;

    @Expose
    @ConfigOption(
        name = "Low Quiver Alert",
        desc = "Notifies you when your quiver\n" +
            "reaches an amount of arrows."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean lowQuiverNotification = false;

    @Expose
    @ConfigOption(
        name = "Reminder After Run",
        desc = "Reminds you to buy arrows after\n" +
            "a Dungeons/Kuudra run if you're low."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean reminderAfterRun = false;

    @Expose
    @ConfigOption(name = "Low Quiver Amount", desc = "")
    @ConfigEditorSlider(minValue = 50, maxValue = 500, minStep = 50)
    public int lowQuiverAmount = 100;
}
