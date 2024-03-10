package at.hannibal2.skyhanni.config.features.fishing;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class TotemOfCorruptionConfig {

    @Expose
    @ConfigOption(name = "Show Overlay", desc = "Show the Totem of Corruption overlay." +
        "\nShows the totem, in which effective area you are in, with the longest time left.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showOverlay = true;

    @Expose
    @ConfigOption(name = "Distance Threshold", desc = "The distance from the player to the Totem of Corruption for the overlay to be shown. " +
        "\nThe effective distance of the totem is 16." +
        "\n§cLimited by how far you can see the nametags.")
    @ConfigEditorSlider(minValue = 0, maxValue = 100, minStep = 1)
    public int distanceThreshold = 16;

    @Expose
    @ConfigOption(name = "Hide Particles", desc = "Hide the particles of the Totem of Corruption.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideParticles = true;

    @Expose
    @ConfigOption(name = "Show Effective Area", desc = "Show the effective area (16 blocks) of the Totem of Corruption.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showEffectiveArea = true;

    @Expose
    @ConfigOption(name = "Color of the area", desc = "The color of the area of the Totem of Corruption.")
    @ConfigEditorColour
    public String color = "0:153:18:159:85";

    @Expose
    public Position position = new Position(50, 20, false, true);
}
