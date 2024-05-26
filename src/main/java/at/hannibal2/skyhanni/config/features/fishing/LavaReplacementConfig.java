package at.hannibal2.skyhanni.config.features.fishing;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import net.minecraft.client.Minecraft;

public class LavaReplacementConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Replace the lava texture with the water texture.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Only In Crimson Isle", desc = "Enable the water texture only in Crimson Isle.")
    @ConfigEditorBoolean
    public boolean onlyInCrimsonIsle = false;

    @ConfigOption(name = "Reload", desc = "Reload renderers.\n§eUse if the lava still appear when the feature is enabled.\n§cMay freeze your game for a few seconds!")
    @ConfigEditorButton(buttonText = "RELOAD")
    public Runnable loadListeners = Minecraft.getMinecraft().renderGlobal::loadRenderers;
}
