package at.hannibal2.skyhanni.config.features.inventory;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class FannCostConfig {

    @Expose
    @ConfigOption(name = "Coins/XP", desc = "Shows coins per XP in Fann menu.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean coinsPerXP = false;

    @Expose
    @ConfigOption(name = "XP/Bits", desc = "Shows XP per bit in Fann menu.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean xpPerBit = false;

    @ConfigEditorInfoText()
    @ConfigOption(name = "Why not Bits/XP?", desc = "One, it's fixed 500 Bits a day, and two, the result would have lots of zeros after decimal.")
    public String whyNot = "";
}
