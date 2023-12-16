package at.hannibal2.skyhanni.config.features.crimsonisle;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class SulphurSkitterBoxConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Box Color", desc = "Choose the color of the box.")
    @ConfigEditorColour
    public String boxColor = "0:255:0:163:36";


}
