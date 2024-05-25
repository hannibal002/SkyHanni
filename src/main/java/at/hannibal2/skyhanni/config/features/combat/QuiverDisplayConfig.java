package at.hannibal2.skyhanni.config.features.combat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;

public class QuiverDisplayConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Show the number of arrows you have.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigLink(owner = QuiverDisplayConfig.class, field = "enabled")
    public Position quiverDisplayPos = new Position(260, -15);

    @Expose
    @ConfigOption(name = "Show arrow icon", desc = "Displays an icon next to the Quiver Display.")
    @ConfigEditorBoolean
    public Property<Boolean> showIcon = Property.of(true);

    @Expose
    @ConfigOption(
        name = "When to show",
        desc = "Decide when to show the display."
    )
    @ConfigEditorDropdown
    public Property<ShowWhen> whenToShow = Property.of(ShowWhen.ONLY_BOW_HAND);

    public enum ShowWhen {
        ALWAYS("Always"),
        ONLY_BOW_INVENTORY("Bow in inventory"),
        ONLY_BOW_HAND("Bow held"),

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
}
