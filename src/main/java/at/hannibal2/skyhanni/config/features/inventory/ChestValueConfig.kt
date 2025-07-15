package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ChestValueConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable estimated value of chest.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Enabled in own Inventory", desc = "Enable the feature for your own inventory.")
    @ConfigEditorBoolean
    var enableInOwnInventory: Boolean = false

    @Expose
    @ConfigOption(name = "Enabled in dungeons", desc = "Enable the feature in dungeons.")
    @ConfigEditorBoolean
    var enableInDungeons: Boolean = false

    @Expose
    @ConfigOption(
        name = "Enable during Item Value",
        desc = "Show this display even if the Estimated Item Value is visible."
    )
    @ConfigEditorBoolean
    var showDuringEstimatedItemValue: Boolean = false

    @Expose
    @ConfigOption(name = "Show Stacks", desc = "Show the item icon before name.")
    @ConfigEditorBoolean
    var showStacks: Boolean = true

    @Expose
    @ConfigOption(name = "Display Type", desc = "Try to align everything to look nicer.")
    @ConfigEditorBoolean
    var alignedDisplay: Boolean = true

    @Expose
    @ConfigOption(
        name = "Name Length",
        desc = "Reduce item name length to gain extra space on screen.\n§cCalculated in pixels!"
    )
    @ConfigEditorSlider(minStep = 1f, minValue = 100f, maxValue = 150f)
    var nameLength: Int = 100

    @Expose
    @ConfigOption(
        name = "Highlight Slot",
        desc = "Highlight slot where the item is when you hover over it in the display."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enableHighlight: Boolean = true

    @Expose
    @ConfigOption(name = "Sorting Type", desc = "Price sorting type.")
    @ConfigEditorDropdown
    var sortingType: SortingTypeEntry = SortingTypeEntry.DESCENDING

    enum class SortingTypeEntry(private val displayName: String) {
        DESCENDING("Descending"),
        ASCENDING("Ascending"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Value formatting Type", desc = "Format of the price.")
    @ConfigEditorDropdown
    var formatType: NumberFormatEntry = NumberFormatEntry.SHORT

    enum class NumberFormatEntry(private val displayName: String) {
        SHORT("Short"),
        LONG("Long"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Item To Show",
        desc = "Choose how many items are displayed.\n" +
            "All items in the chest are still counted for the total value."
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 54f, minStep = 1f)
    var itemToShow: Int = 15

    @Expose
    @ConfigOption(
        name = "Hide below",
        desc = "Hide items with value below configured amount.\n" +
            "Items are still counted for the total value."
    )
    @ConfigEditorSlider(minValue = 50000f, maxValue = 10000000f, minStep = 50000f)
    var hideBelow: Int = 100000

    @Expose
    @ConfigLink(owner = ChestValueConfig::class, field = "enabled")
    val position: Position = Position(107, 141)
}
