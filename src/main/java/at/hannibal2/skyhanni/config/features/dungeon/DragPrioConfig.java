package at.hannibal2.skyhanni.config.features.dungeon;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class DragPrioConfig {
    @Expose
    @ConfigOption(name = "Set Power", desc = "Set the power that you split on.")
    @ConfigEditorSlider(
            minValue = 0,
            maxValue = 32,
            minStep = 1
    )
    public int splitPower = 22;

    @Expose
    @ConfigOption(name = "Easy Power", desc = "Set the power that you split on for easy drags (O/P/G).")
    @ConfigEditorSlider(
            minValue = 0,
            maxValue = 32,
            minStep = 1
    )
    public int easyPower = 19;

    @Expose
    @ConfigOption(name = "Show Non-Split drags", desc = "Display \"X Dragon is spawning!\" on non-split drags.")
    @ConfigEditorBoolean
    public boolean showSingleDragons = true;

    @Expose
    @ConfigOption(name = "Send Split Message", desc = "Send the \"Bers Team: Arch Team:\" message.")
    @ConfigEditorBoolean
    public boolean sendMessage = false;


    @Expose
    @ConfigOption(name = "Say Split", desc = "Send \"Split\" when splitting.")
    @ConfigEditorBoolean
    public boolean saySplit = true;

    @Expose
    @ConfigOption(name = "Healer", desc = "Set the team the healer should be with")
    @ConfigEditorDropdown
    public HealerNormalValue healerNormal = HealerNormalValue.ARCHER;

    public enum HealerNormalValue {
        ARCHER("Archer Team"),
        BERSERK("Berserk Team");

        private final String str;

        HealerNormalValue(String str) {
            this.str = str;
        }

        @Override
        public String toString() { return str; }
    }

    @Expose
    @ConfigOption(name = "Tank", desc = "Set the team the tank should be with")
    @ConfigEditorDropdown
    public TankNormalValue tankNormal = TankNormalValue.ARCHER;

    public enum TankNormalValue {
        ARCHER("Archer Team"),
        BERSERK("Berserk Team");

        private final String str;

        TankNormalValue(String str) {
            this.str = str;
        }

        @Override
        public String toString() { return str; }
    }


    @Expose
    @ConfigOption(name = "Healer Purple", desc = "Set the team the healer should be with when purple")
    @ConfigEditorDropdown
    public HealerPurpleValue healerPurple = HealerPurpleValue.ARCHER;

    public enum HealerPurpleValue {
        ARCHER("Archer Team"),
        BERSERK("Berserk Team");

        private final String str;

        HealerPurpleValue(String str) {
            this.str = str;
        }

        @Override
        public String toString() { return str; }
    }

    @Expose
    @ConfigOption(name = "Tank Purple", desc = "Set the team the tank should be with when purple")
    @ConfigEditorDropdown
    public TankPurpleValue tankPurple = TankPurpleValue.ARCHER;

    public enum TankPurpleValue {
        ARCHER("Archer Team"),
        BERSERK("Berserk Team");

        private final String str;

        TankPurpleValue(String str) {
            this.str = str;
        }

        @Override
        public String toString() { return str; }
    }
}
