package at.hannibal2.skyhanni.config.features.combat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ArmorStackDisplayConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Armor stack display.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Armor Stack Display", desc = "Display the number of stacks on armor pieces like Crimson, Terror, Aurora etc.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean armorStackDisplay = false;

    @Expose
    @ConfigOption(name = "Armor Stack Decay Timer", desc = "Shows a decay timer for armor after gaining a stack.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean armorStackDecayTimer = false;

    @Expose
    @ConfigOption(name = "Show Timer Only For Max Stack", desc = "Shows decay timer only for 10th stack.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean armorStackDecayForMax = false;

    @Expose
    @ConfigLink(owner = ArmorStackDisplayConfig.class, field = "enabled")
    public Position position = new Position(480, -210, 1.9f);
}
