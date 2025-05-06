package at.hannibal2.skyhanni.config.features.garden.composter

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.HasLegacyId
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.utils.ItemPriceSource
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ComposterConfig {
    @Expose
    @ConfigOption(
        name = "Composter Overlay",
        desc = "Show organic matter, fuel, and profit prices while inside the Composter Inventory."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var overlay: Boolean = true

    @Expose
    @ConfigOption(
        name = "Change Price Source",
        desc = "Change what price to use: Bazaar (Sell Offer or Buy Order) or NPC."
    )
    @ConfigEditorDropdown
    var priceSource: ItemPriceSource = ItemPriceSource.BAZAAR_INSTANT_BUY

    @Expose
    @ConfigOption(
        name = "Retrieve From",
        desc = "Change where to retrieve the materials from in the composter overlay: Bazaar or Sacks."
    )
    @ConfigEditorDropdown
    var retrieveFrom: RetrieveFromEntry = RetrieveFromEntry.SACKS

    enum class RetrieveFromEntry(
        private val displayName: String,
        private val legacyId: Int = -1
    ) : HasLegacyId {
        BAZAAR("Bazaar", 0),
        SACKS("Sacks", 1),
        ;

        override fun getLegacyId() = legacyId
        override fun toString() = displayName
    }

    @Expose
    @ConfigLink(owner = ComposterConfig::class, field = "overlay")
    var overlayOrganicMatterPos: Position = Position(140, 152)

    @Expose
    @ConfigLink(owner = ComposterConfig::class, field = "overlay")
    var overlayFuelExtrasPos: Position = Position(-320, 152)

    @Expose
    @ConfigOption(name = "Composter Display", desc = "Display the Composter data from the tab list as GUI element.")
    @ConfigEditorBoolean
    @FeatureToggle
    var displayEnabled: Boolean = false

    @Expose
    @ConfigOption(name = "Outside Garden", desc = "Show Time till Composter is empty outside Garden")
    @ConfigEditorBoolean
    @FeatureToggle
    var displayOutsideGarden: Boolean = false

    @Expose
    @ConfigOption(
        name = "Composter Warning",
        desc = "Warn when the Composter gets close to empty, even outside Garden."
    )
    @ConfigEditorBoolean
    var warnAlmostEmpty: Boolean = false

    @Expose
    @ConfigOption(name = "Upgrade Price", desc = "Show the price for the Composter Upgrade in the lore.")
    @ConfigEditorBoolean
    @FeatureToggle
    var upgradePrice: Boolean = true

    @Expose
    @ConfigOption(
        name = "Round Amount Needed",
        desc = "Round the amount needed to fill your Composter down so that you don't overspend."
    )
    @ConfigEditorBoolean
    var roundDown: Boolean = true

    @Expose
    @ConfigOption(name = "Highlight Upgrade", desc = "Highlight upgrades that can be bought right now.")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightUpgrade: Boolean = true

    @Expose
    @ConfigOption(
        name = "Inventory Numbers",
        desc = "Show the amount of Organic Matter, Fuel and Composts Available while inside the Composter Inventory."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var inventoryNumbers: Boolean = true

    @Expose
    @ConfigOption(name = "Notification When Low Composter", desc = "")
    @Accordion
    var notifyLow: NotifyLowConfig = NotifyLowConfig()

    @Expose
    @ConfigLink(owner = ComposterConfig::class, field = "displayEnabled")
    var displayPos: Position = Position(-390, 10)

    @Expose
    @ConfigLink(owner = ComposterConfig::class, field = "displayEnabled")
    var outsideGardenPos: Position = Position(-363, 13)
}
