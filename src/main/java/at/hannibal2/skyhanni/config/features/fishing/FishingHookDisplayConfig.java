package at.hannibal2.skyhanni.config.features.fishing;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class FishingHookDisplayConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Display the Hypixel timer until the fishing hook can be pulled out of the water/lava, only bigger and on your screen.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(
        name = "Hide Armor Stand",
        desc = "Hide the original armor stand from Hypixel when the SkyHanni display is enabled."
    )
    @ConfigEditorBoolean
    public boolean hideArmorStand = true;

    @Expose
    public Position position = new Position(460, -240, 3.4f);
}
