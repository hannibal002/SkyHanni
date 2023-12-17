package at.hannibal2.skyhanni.config.features.crimsonisle;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.HasLegacyId;
import at.hannibal2.skyhanni.features.nether.SulphurSkitterBox;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class SulphurSkitterBoxConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Render a box around the closest sulphur block.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Box Type", desc = "Choose the look of the box.")
    @ConfigEditorDropdown()
    public BoxType boxType = BoxType.WIREFRAME;

    public enum BoxType implements HasLegacyId {
        FULL("Full"),
        WIREFRAME("Wireframe")
        ;

        private final String str;
        private final int legacyId;

        BoxType(String str, int legacyId) {
            this.str = str;
            this.legacyId = legacyId;
        }

        BoxType(String str) {
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

    @Expose
    @ConfigOption(name = "Box Color", desc = "Choose the color of the box.")
    @ConfigEditorColour
    public String boxColor = "0:102:255:216:0";

    @Expose
    @ConfigOption(name = "Only With Rods", desc = "Render the box only when holding a lava fishing rod.")
    @ConfigEditorBoolean
    public boolean onlyWithRods = true;


}
