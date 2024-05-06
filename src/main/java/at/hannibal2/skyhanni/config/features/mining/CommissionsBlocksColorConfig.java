package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;

public class CommissionsBlocksColorConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Change the color of ores on mining island depending on your active commissions.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Sneak Toggle", desc = "Quickly disable or enable this feature via sneaking.")
    @ConfigEditorBoolean
    public Property<Boolean> sneakQuickToggle = Property.of(false);
}
