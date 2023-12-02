package at.hannibal2.skyhanni.config.features.chroma;

import at.hannibal2.skyhanni.SkyHanniMod;
import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorButton;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorInfoText;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class ChromaConfig {

    @Expose
    @ConfigOption(name = "Chroma Preview", desc = "§fPlease star the mod on GitHub!")
    @ConfigEditorInfoText(infoTitle = "Only In SkyBlock")
    public boolean chromaPreview = false;

    @Expose
    @ConfigOption(name = "Enabled", desc = "Toggle for SkyHanni's chroma. (Disables Patcher's Optimized Font Renderer while enabled)")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Chroma Size", desc = "Change the size of each color in the chroma.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 100f, minStep = 1f)
    public float chromaSize = 30f;

    @Expose
    @ConfigOption(name = "Chroma Speed", desc = "Change how fast the chroma animation moves.")
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 20f, minStep = 0.5f)
    public float chromaSpeed = 6f;

    @Expose
    @ConfigOption(name = "Chroma Saturation", desc = "Change the saturation of the chroma.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 1f, minStep = 0.01f)
    public float chromaSaturation = 0.75f;

    @Expose
    @ConfigOption(name = "Chroma Direction", desc = "Change the slant and direction of the chroma.")
    @ConfigEditorDropdown(values = {"Forward + Right", "Forward + Left", "Backward + Right", "Backward + Left"})
    public int chromaDirection = 0;

    @ConfigOption(name = "Reset to Default", desc = "Resets all chroma settings to the default.")
    @ConfigEditorButton(buttonText = "Reset")
    public Runnable resetSettings = this::resetChromaSettings;

    @Expose
    @ConfigOption(name = "Everything Chroma", desc = "Renders §4§l§oALL §r§7text in chroma. (Some enchants may appear white with SBA enchant parsing)")
    @ConfigEditorBoolean
    public boolean allChroma = false;

    private void resetChromaSettings() {
        SkyHanniMod.getFeature().chroma.chromaSize = 30f;
        SkyHanniMod.getFeature().chroma.chromaSpeed = 6f;
        SkyHanniMod.getFeature().chroma.chromaSaturation = 0.75f;
        SkyHanniMod.getFeature().chroma.allChroma = false;
        SkyHanniMod.getFeature().chroma.chromaDirection = 0;
    }
}
