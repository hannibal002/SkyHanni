package at.hannibal2.skyhanni.config.features.garden.composter;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.HasLegacyId;
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
    @ConfigEditorDropdown
    public OverlayPriceTypeEntry overlayPriceType = OverlayPriceTypeEntry.INSTANT_BUY;

    public enum OverlayPriceTypeEntry implements HasLegacyId {
        INSTANT_BUY("Instant Buy", 0),
        BUY_ORDER("Buy Order", 1),
        ;
        private final String str;
        private final int legacyId;

        OverlayPriceTypeEntry(String str, int legacyId) {
            this.str = str;
            this.legacyId = legacyId;
        }

        // Constructor if new enum elements are added post-migration
        OverlayPriceTypeEntry(String str) {
            this(str, -1);
        }

        @Override
        public int getLegacyId() {
            return legacyId;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @ConfigOption(name = "Retrieve From", desc = "Change where to retrieve the materials from in the composter overlay: The Bazaar or Sacks.")
    @ConfigEditorDropdown
    public RetrieveFromEntry retrieveFrom = RetrieveFromEntry.SACKS;

    public enum RetrieveFromEntry implements HasLegacyId {
        BAZAAR("Bazaar", 0),
        SACKS("Sacks", 1),
        ;
        private final String str;
        private final int legacyId;

        RetrieveFromEntry(String str, int legacyId) {
            this.str = str;
            this.legacyId = legacyId;
        }

        // Constructor if new enum elements are added post-migration
        RetrieveFromEntry(String str) {
            this(str, -1);
        }

        @Override
        public int getLegacyId() {
            return legacyId;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    public Position overlayOrganicMatterPos = new Position(140, 152, false, true);

    @Expose
    public Position overlayFuelExtrasPos = new Position(-320, 152, false, true);

    @Expose
    @ConfigOption(
        name = "Composter Display",
        desc = "Displays the Composter data from the tab list as GUI element."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean displayEnabled = false;

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
