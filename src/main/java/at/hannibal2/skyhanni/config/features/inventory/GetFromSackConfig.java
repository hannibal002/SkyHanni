package at.hannibal2.skyhanni.config.features.inventory;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class GetFromSackConfig {

    @Expose
    @ConfigOption(name = "Queued GfS", desc = "If §e/gfs §7or §e/getfromsacks §7is used it queues up the commands so all items are guarantied to be received.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean queuedGFS = true;

    @Expose
    @ConfigOption(name = "Bazaar GfS", desc = "If you don't have enough items in sack get a prompt to buy them from bazaar.")
    @ConfigEditorBoolean
    public boolean bazaarGFS = false;
}
