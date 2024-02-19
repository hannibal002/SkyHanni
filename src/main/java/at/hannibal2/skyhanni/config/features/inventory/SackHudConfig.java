package at.hannibal2.skyhanni.config.features.inventory;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.features.inventory.SackHud;
import at.hannibal2.skyhanni.utils.NEUInternalName;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorButton;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.List;

public class SackHudConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Display a HUD with your sack contents.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @ConfigOption(
        name = "Configure",
        desc = "Configure the items display in your Sack HUD"
    )
    @ConfigEditorButton
    public Runnable configure = SackHud::openConfigureScreen;

    @Expose
    public List<NEUInternalName> trackedItems = new ArrayList<>();

    @Expose
    public Position position = new Position(144, 139, false, true);
}
