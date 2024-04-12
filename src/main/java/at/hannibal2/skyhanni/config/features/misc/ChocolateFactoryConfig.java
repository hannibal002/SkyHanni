package at.hannibal2.skyhanni.config.features.misc;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ChocolateFactoryConfig {

    @Expose
    @ConfigOption(name = "Show Stack Sizes", desc = "Shows addition info as many items in the chocolate menu as the stack size.")
    @ConfigEditorBoolean
    public boolean showStackSizes = true;

    @Expose
    @ConfigOption(name = "Highlight Upgrades", desc = "Highlight any upgrades that you can afford.")
    @ConfigEditorBoolean
    public boolean highlightUpgrades = true;

}
