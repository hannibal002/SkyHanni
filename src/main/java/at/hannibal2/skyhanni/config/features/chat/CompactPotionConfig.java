package at.hannibal2.skyhanni.config.features.chat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class CompactPotionConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Shorten chat messages about player potion effects.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Clickable Chat Message", desc = "Makes the Compact Potion message open the Potion effects menu on click.")
    @ConfigEditorBoolean
    public boolean clickableChatMessage = true;
}
