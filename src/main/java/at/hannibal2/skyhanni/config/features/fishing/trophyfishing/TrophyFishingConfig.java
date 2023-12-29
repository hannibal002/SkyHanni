package at.hannibal2.skyhanni.config.features.fishing.trophyfishing;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class TrophyFishingConfig {

    @Expose
    @ConfigOption(name = "Trophy Fishing Chat Messages", desc = "")
    @Accordion
    public ChatMessagesConfig chatMessages = new ChatMessagesConfig();

    @Expose
    @ConfigOption(name = "Fillet Tooltip", desc = "Show fillet value of Trophy Fish in tooltip.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean filletTooltip = true;

    @Expose
    @ConfigOption(
        name = "Odger Waypoint",
        desc = "Show the Odger waypoint when Trophy Fishes are in the inventory and no lava rod in hand. " +
            "§cOnly useful for users without abiohone contact."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean odgerLocation = true;
}
