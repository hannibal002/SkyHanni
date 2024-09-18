package at.hannibal2.skyhanni.config.features.combat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ArmorStackDisplayConfig {
    @Expose
    @ConfigOption(name = "Enable Display", desc = "Enable the armor stack display feature.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Show Armor Stack", desc = "Display the number of stacks for armor pieces like Crimson, Terror, Aurora, etc.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean armorStackDisplay = false;

    @Expose
    @ConfigOption(name = "Stack Decay Timer", desc = "Show a decay timer after gaining a stack.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean armorStackDecayTimer = false;

    @Expose
    @ConfigOption(name = "Show Timer for Max Stack", desc = "Show the decay timer only when the 10th stack is reached.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean armorStackDecayForMax = false;

    @Expose
    @ConfigLink(owner = ArmorStackDisplayConfig.class, field = "enabled")
    public Position position = new Position(480, -210, 1.9f);
}
