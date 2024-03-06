package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class PotionEffectsConfig {
    @Expose
    @ConfigOption(name = "Non God Pot Effects", desc = "Display the active potion effects that are not part of the God Pot.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean nonGodPotEffectDisplay = false;

    @Expose
    @ConfigOption(name = "Show Mixins", desc = "Include God Pot mixins in the Non God Pot Effects display.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean nonGodPotEffectShowMixins = false;

    @Expose
    public Position nonGodPotEffectPos = new Position(10, 10, false, true);
}
