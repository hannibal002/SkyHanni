package at.hannibal2.skyhanni.features.inventory.caketracker;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class CakeTrackerConfig {

    @Expose
    @ConfigOption(name = "New Year Cake Tracker", desc = "Track which New Year Cakes you have/need.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigLink(owner = CakeTrackerConfig.class, field = "enabled")
    public Position cakeTrackerPosition = new Position(300, 300, false, true);

    @Expose
    @ConfigOption(name = "Display Mode", desc = "How the tracker should display")
    public CakeTrackerDisplayType displayType = CakeTrackerDisplayType.OLDEST_FIRST;

    public enum CakeTrackerDisplayType {

        OLDEST_FIRST("§cOldest Missing First"),
        NEWEST_FIRST("§dNewest Missing First")
        ;

        private final String name;

        CakeTrackerDisplayType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
