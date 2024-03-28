package at.hannibal2.skyhanni.config.features.combat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class DragonConfig {

    @Expose
    @ConfigOption(name = "Superior Notification", desc = "Notifies you with an Title that an superior dragon spawned")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean superiorNotify = true;

    @Expose
    @ConfigOption(name = "Weight HUD", desc = "Shows your current dragon weight on the HUD and if hovered shows the breakdown")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean display = false;

    @Expose
    public Position displayPosition = new Position(120, 40, false, true);

    @Expose
    @ConfigOption(name = "Weight Message", desc = "Shows your dragon weight in chat after the dragon died")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean chat = true;
}
