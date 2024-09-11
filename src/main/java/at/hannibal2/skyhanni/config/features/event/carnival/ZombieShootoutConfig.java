package at.hannibal2.skyhanni.config.features.event.carnival;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.features.gui.HotbarConfig;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

import java.awt.Color;

public class ZombieShootoutConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "QOL Features for Zombie Shootout.")
    @FeatureToggle
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Colored Hitboxes", desc = "Display colored hitboxes for zombies and lanterns.")
    @ConfigEditorBoolean
    public boolean coloredHitboxes = true;

    @Expose
    @ConfigOption(name = "Highest Only", desc = "Only draw colored hitboxes for the highest scoring zombies.")
    @ConfigEditorBoolean
    public boolean highestOnly = false;

    @Expose
    @ConfigOption(name = "Lantern Timer", desc = "Show time until lantern disappears.")
    @ConfigEditorBoolean
    public boolean lanternTimer = true;

    @Expose
    @ConfigLink(owner = ZombieShootoutConfig.class, field = "lanternTimer")
    public Position lanternPosition = new Position(20, 20, false, true);
}
