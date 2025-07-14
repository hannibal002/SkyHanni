package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.RenderUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SackDisplayConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show contained items inside a sack inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Highlight Full",
        desc = "Highlight items that are full in red.\n" +
            "§eDoes not need the option above to be enabled."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightFull: Boolean = true

    @Expose
    @ConfigOption(
        name = "Number Format",
        desc = "Either show Default, Formatted or Unformatted numbers.\n" +
            "§eDefault: §72,240/2.2k\n" +
            "§eFormatted: §72.2k/2.2k\n" +
            "§eUnformatted: §72,240/2,200"
    )
    @ConfigEditorDropdown
    var numberFormat: NumberFormatEntry = NumberFormatEntry.FORMATTED

    @Expose
    @ConfigOption(name = "Alignment", desc = "Change the alignment for numbers and money.")
    @ConfigEditorDropdown
    var alignment: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT

    enum class NumberFormatEntry(private val displayName: String) {
        DEFAULT("Default"),
        FORMATTED("Formatted"),
        UNFORMATTED("Unformatted"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Extra space", desc = "Space between each line of text.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 10f, minStep = 1f)
    var extraSpace: Int = 1

    @Expose
    @ConfigOption(name = "Sorting Type", desc = "Sorting type of items in sack.")
    @ConfigEditorDropdown
    var sortingType: SortingTypeEntry = SortingTypeEntry.DESC_STORED

    enum class SortingTypeEntry(private val displayName: String) {
        DESC_STORED("Stored Descending"),
        ASC_STORED("Stored Ascending"),
        DESC_PRICE("Price Descending"),
        ASC_PRICE("Price Ascending"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Item To Show",
        desc = "Choose how many items are displayed. (Some sacks have too many items to fit\n" +
            "in larger GUI scales, like the nether sack.)"
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 45f, minStep = 1f)
    var itemToShow: Int = 15

    @Expose
    @ConfigOption(name = "Show Empty Item", desc = "Show empty item quantity in the display.")
    @ConfigEditorBoolean
    var showEmpty: Boolean = true

    @Expose
    @ConfigOption(name = "Show Price", desc = "Show price for each item in sack.")
    @ConfigEditorBoolean
    var showPrice: Boolean = true

    @Expose
    @ConfigOption(
        name = "Price Format",
        desc = "Format of the price displayed.\n" +
            "§eFormatted: §7(12k)\n" +
            "§eUnformatted: §7(12,421)"
    )
    @ConfigEditorDropdown
    var priceFormat: PriceFormatEntry = PriceFormatEntry.FORMATTED

    enum class PriceFormatEntry(private val displayName: String) {
        FORMATTED("Formatted"),
        UNFORMATTED("Unformatted"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Change Price Source",
        desc = "Change what price to use: Bazaar (Sell Offer or Buy Order) or NPC."
    )
    @ConfigEditorDropdown
    var priceSource: ItemPriceSource = ItemPriceSource.BAZAAR_INSTANT_BUY

    @Expose
    @ConfigLink(owner = SackDisplayConfig::class, field = "enabled")
    val position: Position = Position(144, 139)
}
