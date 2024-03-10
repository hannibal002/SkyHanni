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
    @ConfigOption(name = "Min Distance", desc = "Always shows mobs that are at least that close to the player.")
    @ConfigEditorSlider(minValue = 3, maxValue = 30, minStep = 1)
    public int minDistance = 10;

    @Expose
    @ConfigOption(name = "Max Amount", desc = "Not showing more than this amount of nearest entities.")
    @ConfigEditorSlider(minValue = 1, maxValue = 150, minStep = 1)
    public int maxAmount = 30;
}
