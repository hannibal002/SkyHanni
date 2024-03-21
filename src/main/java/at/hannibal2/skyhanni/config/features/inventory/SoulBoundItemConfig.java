package at.hannibal2.skyhanni.config.features.inventory;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class SoulBoundItemConfig {

    @Expose
    @ConfigOption(name = "Symbol", desc = "Shows a symbol next to item to indicate if it is soulbound.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean icon = false;

    @Expose
    @ConfigOption(name = "Remove Soulbound Text", desc = "Removes soulbound text from the bottom of the item.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean text = false;
}
