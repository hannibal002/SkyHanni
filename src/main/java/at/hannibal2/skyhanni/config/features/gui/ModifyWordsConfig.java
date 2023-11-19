package at.hannibal2.skyhanni.config.features.gui;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.commands.Commands;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorButton;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class ModifyWordsConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Enables replacing all instances of a word or phrase with another word or phrase.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Work Outside SkyBlock", desc = "Allows modifying visual words anywhere on Hypixel.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean workOutside = false;

    @ConfigOption(name = "Open Config", desc = "Opens the menu to setup the visual words.\n§eCommand: /shwords")
    @ConfigEditorButton(buttonText = "Open")
    public Runnable open = Commands::openVisualWords;

}
