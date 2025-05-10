package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.utils.ItemPriceSource
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import org.lwjgl.input.Keyboard

class EstimatedItemValueConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Display an Estimated Item Value for the item you hover over.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Show on Tooltip", desc = "Puts the estimated item value in the tooltip.")
    @ConfigEditorBoolean
    var showTooltip: Boolean = false

    @Expose
    @ConfigOption(name = "Hotkey", desc = "Press this key to show the Estimated Item Value.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var hotkey: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(name = "Show Always", desc = "Ignore the hotkey and always display the item value.")
    @ConfigEditorBoolean
    var alwaysEnabled: Boolean = true

    @Expose
    @ConfigOption(name = "Enchantments Cap", desc = "Only show the top # most expensive enchantments.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 30f, minStep = 1f)
    var enchantmentsCap: Property<Int> = Property.of(7)

    @Expose
    @ConfigOption(name = "Star Material Cap", desc = "Only show the top # most expensive parts of star prices.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 15f, minStep = 1f)
    var starMaterialCap: Property<Int> = Property.of(3)

    @Expose
    @ConfigOption(name = "Show Exact Price", desc = "Show the exact total price instead of the compact number.")
    @ConfigEditorBoolean
    var exactPrice: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Show Armor Value", desc = "Show the value of the full armor set in the Wardrobe inventory.")
    @ConfigEditorBoolean
    var armor: Boolean = true

    @Expose
    @ConfigOption(name = "Ignore Helmet Skins", desc = "Ignore helmet Skins from the total value.")
    @ConfigEditorBoolean
    var ignoreHelmetSkins: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Ignore Armor Dyes", desc = "Ignore Armor Dyes from the total value.")
    @ConfigEditorBoolean
    var ignoreArmorDyes: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Ignore Runes", desc = "Ignore Runes from the total value.")
    @ConfigEditorBoolean
    var ignoreRunes: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Change Price Source",
        desc = "Change what price to use: Bazaar (Sell Offer or Buy Order) or NPC."
    )
    @ConfigEditorDropdown
    var priceSource: Property<ItemPriceSource> = Property.of(ItemPriceSource.BAZAAR_INSTANT_SELL)

    enum class BazaarPriceSource(private val displayName: String) {
        INSTANT_BUY("Instant Buy"),
        BUY_ORDER("Buy Order"),
        ;

        override fun toString(): String {
            return displayName
        }
    }

    @Expose
    @ConfigOption(
        name = "Use Attribute Price",
        desc = "Show composite price for attributes instead of lowest bin. This will drastically decrease the " +
            "estimated value but might be correct when buying multiple low tier items and combining them."
    )
    @ConfigEditorBoolean
    var useAttributeComposite: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Ignore Attributes",
        desc = "Ignore attribute prices when calculating the total cost of an item."
    )
    @ConfigEditorBoolean
    var ignoreAttributes: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigLink(owner = EstimatedItemValueConfig::class, field = "enabled")
    var itemPriceDataPos: Position = Position(140, 90) // TODO rename "position"
}
