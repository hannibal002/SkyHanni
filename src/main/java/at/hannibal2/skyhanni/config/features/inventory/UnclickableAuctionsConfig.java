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
    @ConfigOption(
        name = "Opacity",
        desc = "How strong should the items be grayed out?"
    )
    @ConfigEditorSlider(
        minValue = 0,
        maxValue = 255,
        minStep = 5
    )
    public int opacity = 180;

    @Expose
    @ConfigOption(name = "Bypass With Key", desc = "Adds the ability to bypass unclickable auctions when holding a keybind of your choice.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int bypassKey = Keyboard.KEY_NONE;

}
