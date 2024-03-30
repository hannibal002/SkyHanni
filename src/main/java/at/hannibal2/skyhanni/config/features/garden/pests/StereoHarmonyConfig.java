package at.hannibal2.skyhanni.config.features.garden.pests;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class StereoHarmonyConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Shows a display of what pest is being boosted by your vinyl."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public Property<Boolean> displayEnabled = Property.of(true);

    @Expose
    @ConfigOption(name = "Display Type", desc = "")
    @ConfigEditorDropdown
    public Property<DisplayType> displayType = Property.of(DisplayType.HEAD);

    public enum DisplayType {
        BOTH("Both"),
        HEAD("Show Head"),
        NAME("Show Name"),

        NONE("None"),

        ;
        private final String str;

        DisplayType(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }

    }

    @Expose
    @ConfigOption(
        name = "Hide when None",
        desc = "hide when no vinyl selected selected."
    )
    @ConfigEditorBoolean
    public Property<Boolean> hideWhenNone = Property.of(false);

    @Expose
    public Position position = new Position(315, -200, 1f);
}
