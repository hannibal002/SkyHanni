package at.hannibal2.skyhanni.config.features.event;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class MayorJerryConfig {

    @Expose
    @ConfigOption(name = "Highlight Jerries", desc = "Highlights Jerries found from the Jerrypocalypse perk. Highlight color is based on color of the Jerry.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightJerries = true;

}
