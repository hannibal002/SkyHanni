package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class QuickModMenuSwitchConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Adding a mod list, allowing to quickly switch between different mod menus.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Inside Escape Menu", desc = "Show the mod list while inside the Escape menu.")
    @ConfigEditorBoolean
    public boolean insideEscapeMenu = true;

    @Expose
    @ConfigOption(name = "Inside Inventory", desc = "Show the mod list while inside the player inventory (no chest inventory).")
    @ConfigEditorBoolean
    public boolean insidePlayerInventory = false;

    @Expose
    public Position pos = new Position(-178, 143, false, true);
}
