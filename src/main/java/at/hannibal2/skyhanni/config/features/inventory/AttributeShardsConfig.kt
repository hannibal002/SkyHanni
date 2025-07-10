package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.inventory.attribute.AttributeShardOverlay
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class AttributeShardsConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Enables the attribute shard overlay that helps you find the best attributes to level up.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Hide maxed", desc = "Hide maxed attribute shards.")
    @ConfigEditorBoolean
    var hideMaxed: Boolean = true

    @Expose
    @ConfigOption(name = "Only not unlocked", desc = "Only show not unlocked shards.")
    @ConfigEditorBoolean
    var onlyNotUnlocked: Boolean = false

    @Expose
    @ConfigOption(name = "Display Sorting Method", desc = "The method used to sort the attribute shards in the overlay.")
    @ConfigEditorDropdown
    var displaySortingMethod: AttributeShardOverlay.AttributeShardSorting = AttributeShardOverlay.AttributeShardSorting.PRICE_TO_NEXT_TIER

    @Expose
    @ConfigOption(name = "Overlay Price Source", desc = "The price source used for the attribute shard overlay.")
    @ConfigEditorDropdown
    var overlayPriceSource: AttributeShardOverlay.AttributeShardPriceSource = AttributeShardOverlay.AttributeShardPriceSource.INSTANT_BUY

    @Expose
    @ConfigOption(name = "Tier As Stack Size", desc = "Display the tier of the shard as stack size in the attribute menu.")
    @ConfigEditorBoolean
    @FeatureToggle
    var tierAsStackSize: Boolean = true

    @Expose
    @ConfigOption(name = "Highlight Disabled Attributes", desc = "Highlight disabled attributes in /attributemenu.")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightDisabledAttributes: Boolean = true

    @Expose
    @ConfigOption(name = "Hunting Box Value", desc = "Displays the bazaar price of the shards in your hunting box.")
    @ConfigEditorBoolean
    @FeatureToggle
    var huntingBoxValue: Boolean = true

    @Expose
    @ConfigLink(owner = AttributeShardsConfig::class, field = "enabled")
    val displayPosition: Position = Position(174, 139)

    @Expose
    @ConfigLink(owner = AttributeShardsConfig::class, field = "huntingBoxValue")
    val huntingBoxValuePosition = Position(174, 139)

}
