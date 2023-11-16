package at.hannibal2.skyhanni.config.features.garden.visitor;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class NeedsConfig {
    @Expose
    @ConfigOption(name = "Items Needed", desc = "Show all items needed for the visitors.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean display = true;

    @Expose
    public Position pos = new Position(180, 170, false, true);

    @Expose
    @ConfigOption(name = "Only when Close", desc = "Only show the needed items when close to the visitors.")
    @ConfigEditorBoolean
    public boolean onlyWhenClose = false;

    @Expose
    @ConfigOption(name = "Bazaar Alley", desc = "Show the Visitor Items List while inside the Bazaar Alley in the Hub. " +
        "This helps buying the correct amount when not having a Booster Cookie Buff active.")
    @ConfigEditorBoolean
    public boolean inBazaarAlley = true;

    @Expose
    @ConfigOption(name = "Show Price", desc = "Show the coin price in the items needed list.")
    @ConfigEditorBoolean
    public boolean showPrice = true;

    @Expose
    @ConfigOption(name = "Item Preview", desc = "Show the base type for the required items next to new visitors. §cNote that some visitors may require any crop.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean itemPreview = true;
}
