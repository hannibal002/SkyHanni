package at.hannibal2.skyhanni.config.features.itemability;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class ChickenHeadConfig {

    @Expose
    @ConfigOption(name = "Checken Head Timer", desc = "Show the cooldown until the next time you can lay an egg with the Chicken Head.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean displayTimer = false;

    @Expose
    public Position position = new Position(-372, 73, false, true);

    @Expose
    @ConfigOption(name = "Hide Chat", desc = "Hide the 'You laid an egg!' chat message.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideChat = true;
}
