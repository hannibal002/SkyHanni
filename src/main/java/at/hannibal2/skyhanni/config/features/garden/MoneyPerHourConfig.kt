package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MoneyPerHourConfig {
    @Expose
    @ConfigOption(
        name = "Show Money per Hour",
        desc = "Display the money per hour YOU get with YOUR crop/minute value when selling the item to bazaar.\n" +
            "Supports Bountiful, Mushroom Cow Perk, Armor Crops and Dicer Drops. Their toggles are below."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var display: Boolean = false

    // TODO Write ConditionalUtils.onToggle()-s for these values in their feature classes
    @Expose
    @ConfigOption(name = "Only Show Top", desc = "Only show the best # items.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 25f, minStep = 1f)
    var showOnlyBest: Int = 5

    @Expose
    @ConfigOption(
        name = "Extend Top List",
        desc = "Add current crop to the list if its lower ranked than the set limit by extending the list."
    )
    @ConfigEditorBoolean
    var showCurrent: Boolean = true

    // TODO Write ConditionalUtils.onToggle()-s for these values in their feature classes
    @Expose
    @ConfigOption(name = "Always On", desc = "Always show the money/hour Display while in the garden.")
    @ConfigEditorBoolean
    var alwaysOn: Boolean = false

    @Expose
    @ConfigOption(name = "Compact Mode", desc = "Hide the item name and the position number.")
    @ConfigEditorBoolean
    var compact: Boolean = false

    @Expose
    @ConfigOption(name = "Compact Price", desc = "Show the price more compact.")
    @ConfigEditorBoolean
    var compactPrice: Boolean = false

    @Expose
    @ConfigOption(
        name = "Use Custom",
        desc = "Use the custom format below instead of classic ➜ §eSell Offer §7and other profiles ➜ §eNPC Price."
    )
    @ConfigEditorBoolean
    var useCustomFormat: Boolean = false

    @Expose
    @ConfigOption(name = "Custom Format", desc = "Set what prices to show")
    @ConfigEditorDraggableList(requireNonEmpty = true)
    val customFormat: MutableList<CustomFormatEntry> = mutableListOf(
        CustomFormatEntry.SELL_OFFER,
        CustomFormatEntry.INSTANT_SELL,
        CustomFormatEntry.NPC_PRICE
    )

    enum class CustomFormatEntry(private val displayName: String) {
        SELL_OFFER("§eSell Offer"),
        INSTANT_SELL("§eInstant Sell"),
        NPC_PRICE("§eNPC Price"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Merge Seeds", desc = "Merge the seeds price with the wheat price.")
    @ConfigEditorBoolean
    var mergeSeeds: Boolean = true

    @Expose
    @ConfigOption(name = "Include Bountiful", desc = "Include the coins from Bountiful in the calculation.")
    @ConfigEditorBoolean
    var bountiful: Boolean = true

    @Expose
    @ConfigOption(
        name = "Include Mooshroom Cow",
        desc = "Include the coins you get from selling the mushrooms from your Mooshroom Cow pet."
    )
    @ConfigEditorBoolean
    var mooshroom: Boolean = true

    @Expose
    @ConfigOption(name = "Include Armor Drops", desc = "Include the average coins/hr from your armor.")
    @ConfigEditorBoolean
    var armor: Boolean = true

    @Expose
    @ConfigOption(name = "Include Dicer Drops", desc = "Include the average coins/hr from your melon or pumpkin dicer.")
    @ConfigEditorBoolean
    var dicer: Boolean = true

    @Expose
    @ConfigOption(name = "Hide Title", desc = "Hide the first line of 'Money Per Hour' entirely.")
    @ConfigEditorBoolean
    var hideTitle: Boolean = false

    // Todo rename to position
    @Expose
    @ConfigLink(owner = MoneyPerHourConfig::class, field = "display")
    val pos: Position = Position(-330, 170)
}
