package at.hannibal2.skyhanni.config.features.garden.composter;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class ComposterConfig {
    @Expose
    @ConfigOption(
        name = "Composter Overlay",
        desc = "Show organic matter, fuel, and profit prices while inside the Composter Inventory."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean overlay = true;

    @Expose
    @ConfigOption(name = "Overlay Price", desc = "Toggle for Bazaar 'buy order' vs 'instant buy' price in composter overlay.")
    @ConfigEditorDropdown(values = {"Instant Buy", "Buy Order"})
    public int overlayPriceType = 0;

    @Expose
    @ConfigOption(name = "Retrieve From", desc = "Change where to retrieve the materials from in the composter overlay: The Bazaar or Sacks.")
    @ConfigEditorDropdown(values = {"Bazaar", "Sacks"})
    public int retrieveFrom = 0;

    @Expose
    public Position overlayOrganicMatterPos = new Position(140, 152, false, true);

    @Expose
    public Position overlayFuelExtrasPos = new Position(-320, 152, false, true);

    @Expose
    @ConfigOption(
        name = "Display Element",
        desc = "Displays the Compost data from the tab list as GUI element."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean displayEnabled = true;

    @Expose
    @ConfigOption(
        name = "Outside Garden",
        desc = "Show Time till Composter is empty outside Garden"
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean displayOutsideGarden = false;

    @Expose
    @ConfigOption(
        name = "Composter Warning",
        desc = "Warn when the Composter gets close to empty, even outside Garden."
    )
    @ConfigEditorBoolean
    public boolean warnAlmostClose = false;

    @Expose
    @ConfigOption(
        name = "Upgrade Price",
        desc = "Show the price for the Composter Upgrade in the lore."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean upgradePrice = true;

    @Expose
    @ConfigOption(
        name = "Round Amount Needed",
        desc = "Rounds the amount needed to fill your Composter down so that you don't overspend."
    )
    @ConfigEditorBoolean
    public boolean roundDown = true;

    @Expose
    @ConfigOption(
        name = "Highlight Upgrade",
        desc = "Highlight Upgrades that can be bought right now."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightUpgrade = true;

    @Expose
    @ConfigOption(
        name = "Inventory Numbers",
        desc = "Show the amount of Organic Matter, Fuel and Composts Available while inside the Composter Inventory."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean inventoryNumbers = true;

    @Expose
    @ConfigOption(name = "Notification When Low Composter", desc = "")
    @Accordion
    public NotifyLowConfig notifyLow = new NotifyLowConfig();

    @Expose
    public Position displayPos = new Position(-390, 10, false, true);

    @Expose
    public Position outsideGardenPos = new Position(-363, 13, false, true);
}
