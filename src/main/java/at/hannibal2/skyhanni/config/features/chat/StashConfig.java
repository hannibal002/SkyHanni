package at.hannibal2.skyhanni.config.features.chat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class StashConfig {

    @Expose
    @ConfigOption(name = "Stash Warnings", desc = "Compact warnings relating to items/materials in your stash.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @ConfigOption(
        name = "§cNotice",
        desc = "Due to Hypixel sending empty messages before and after the stash message, " +
            "you may see empty lines still. Turn on `/sh empty messages` to solve for this."
    )
    @ConfigEditorInfoText
    public String notice = "";

    @Expose
    @ConfigOption(name = "Hide Duplicate Warnings", desc = "Hide duplicate warnings for previously reported stash counts.")
    @ConfigEditorBoolean
    public boolean hideDuplicateCounts = true;

    @Expose
    @ConfigOption(name = "Hide Low Warnings", desc = "Hide warnings with a total count below this number.")
    @ConfigEditorSlider(minValue = 0, maxValue = 1000000, minStep = 100)
    public int hideLowWarningsThreshold = 0;

    @Expose
    @ConfigOption(name = "Use /ViewStash", desc = "Use /viewstash [type] instead of /pickupstash.")
    @ConfigEditorBoolean
    public boolean useViewStash = false;
}
