package at.hannibal2.skyhanni.config.features.garden.laneswitch;

import at.hannibal2.skyhanni.config.HasLegacyId;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class LaneswitchFarmConfig {
    @Expose
    @ConfigOption(name = "Lane Length", desc = "The length (in plots) of each lane.")
    @ConfigEditorSlider(
        minValue = 1,
        maxValue = 5,
        minStep = 1
    )
    public int plotAmount = 1;

    @ConfigOption(name = "Farm direction", desc = "The direction you are facing while farming.")
    @Expose
    @ConfigEditorDropdown
    public FarmDirection farmDirection = FarmDirection.NORTH_SOUTH;

    public enum FarmDirection{
        NORTH_SOUTH("North-South"),
        EAST_WEST("East-West");
        private final String str;

        FarmDirection(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
