package at.hannibal2.skyhanni.config.features.garden;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

public class EliteWebsiteConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Enables opening past contests in §eelitebot.dev§7.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "EliteWebsite Key", desc = "Click on a past farming contest while holding a custom keybind to open its corresponding page in §eelitebot.dev§7.\n§4For optimal experiences, do §lNOT§r §4bind this to a mouse button.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int eliteWebsiteKeybind = Keyboard.KEY_NONE;
}
