package at.hannibal2.skyhanni.config.features.dev;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class HideFarEntitiesConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Hide all entities from rendering except the nearest ones.")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Amount", desc = "Keep showing this amount of nearest entities.")
    @ConfigEditorSlider(minValue = 5, maxValue = 150, minStep = 5)
    public int amount = 50;
}
