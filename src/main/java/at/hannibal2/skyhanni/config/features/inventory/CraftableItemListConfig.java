package at.hannibal2.skyhanni.config.features.inventory;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class CraftableItemListConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Shows a list of items that can be crafted with the items in inventory when inside the crafting menu. " +
            "Click on the item to open §e/recipe§7."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(
        name = "Include Sacks",
        desc = "Include items from inside the sacks.")
    @ConfigEditorBoolean
    public boolean includeSacks = false;

    @Expose
    @ConfigOption(
        name = "Include Vanilla Items",
        desc = "Include vanilla items.")
    @ConfigEditorBoolean
    public boolean includeVanillaItems = false;

    @Expose
    public Position position = new Position(144, 139, false, true);
}
