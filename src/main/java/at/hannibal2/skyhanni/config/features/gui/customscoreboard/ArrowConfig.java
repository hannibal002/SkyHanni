package at.hannibal2.skyhanni.config.features.gui.customscoreboard;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class ArrowConfig {
    @Expose
    @ConfigOption(name = "Arrow Amount Display", desc = "Determines how the arrow amount is displayed.")
    @ConfigEditorDropdown
    public ArrowAmountDisplay arrowAmountDisplay = ArrowAmountDisplay.NUMBER;

    public enum ArrowAmountDisplay {
        NUMBER("Number"),
        PERCENTAGE("Percentage"),
        ;

        private final String str;

        ArrowAmountDisplay(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @ConfigOption(name = "Color Arrow Amount", desc = "Color the arrow amount based on the percentage.")
    @ConfigEditorBoolean
    public boolean colorArrowAmount = false;
}
