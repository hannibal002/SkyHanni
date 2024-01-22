package at.hannibal2.skyhanni.config.features.inventory;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

public class UnclickableAuctionsConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Prevents buying/bidding on auctions for recombobulated items and tier-boosted pets.")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Opacity", desc = "Adjust the visibility of items being blurred out.")
    @ConfigEditorSlider(
        minValue = 0,
        maxValue = 255,
        minStep = 5
    )
    public int opacity = 180;

    @Expose
    @ConfigOption(name = "Bypass With Key", desc = "Bypass unclickable auctions when holding a keybind of your choice." +
        "\n§eBy setting this keybind to \"NONE\", you are disabling the bypass entirely.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int bypassKey = Keyboard.KEY_NONE;

}
