package at.hannibal2.skyhanni.config.features.gui;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class QuiverDisplayConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Show the number of arrows you have.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    public Position quiverDisplayPos = new Position(260, 80);

    @Expose
    @ConfigOption(name = "Show arrow icon", desc = "Displays an icon next to the Quiver Display.")
    @ConfigEditorBoolean
    public Property<Boolean> showIcon = Property.of(true);

    @Expose
    @ConfigOption(
        name = "When to show the display",
        desc = ""
    )
    @ConfigEditorDropdown
    public Property<ShowWhen> whenToShow = Property.of(ShowWhen.ONLY_BOW_INVENTORY);

    public enum ShowWhen {
        ALWAYS("Always"),
        ONLY_BOW_INVENTORY("Only with Bow in inventory"),
        ONLY_BOW_HAND("Only with Bow in hand"),

        ;
        private final String str;

        ShowWhen(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

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
    @ConfigOption(name = "Low Quiver Amount", desc = "Amount at which to notify you.")
    @ConfigEditorSlider(minValue = 50, maxValue = 500, minStep = 50)
    public int lowQuiverAmount = 100;
}
