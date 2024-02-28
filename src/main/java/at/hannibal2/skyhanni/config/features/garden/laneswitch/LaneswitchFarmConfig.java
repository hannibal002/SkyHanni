package at.hannibal2.skyhanni.config.features.garden.laneswitch;

import at.hannibal2.skyhanni.config.HasLegacyId;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class LaneswitchFarmConfig {
    @Expose
    @ConfigOption(name = "Amount of Plots", desc = "The amount of plots the current Farm has.")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 5,
        minStep = 1
    )
    public int plotAmount = 1;

    @ConfigOption(name = "Farm Direction", desc = "The Direction you face while you are farming.")
    @Expose
    @ConfigEditorDropdown
    public FarmDirection farmDirection = FarmDirection.SOUTH;

    public enum FarmDirection implements HasLegacyId {
        NORTH("North", 0),
        EAST("East", 1),
        SOUTH("South", 2),
        WEST("West", 3);
        private final String str;
        private final int legacyId;

        FarmDirection(String str, int legacyId) {
            this.str = str;
            this.legacyId = legacyId;
        }

        // Constructor if new enum elements are added post-migration
        FarmDirection(String str) {
            this(str, -1);
        }

        @Override
        public int getLegacyId() {
            return legacyId;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
