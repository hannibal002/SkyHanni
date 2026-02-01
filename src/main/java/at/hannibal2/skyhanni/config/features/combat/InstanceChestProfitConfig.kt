package at.hannibal2.skyhanni.config.features.combat

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.utils.ItemPriceSource
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW

class InstanceChestProfitConfig {
    // TODO since this feature toggle no longer enables the whole category, it should be renamed
    @Expose
    @ConfigOption(name = "Instance Chest Profit", desc = "Display chest profit for dungeons and kuudra.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Change Price Source",
        desc = "Change what price to use: Bazaar (Sell Offer or Buy Order) or NPC.",
    )
    @ConfigEditorDropdown
    var priceSource: ItemPriceSource = ItemPriceSource.BAZAAR_INSTANT_SELL

    @Expose
    @ConfigLink(owner = InstanceChestProfitConfig::class, field = "enabled")
    val position: Position = Position(107, 141)

    @Expose
    @ConfigOption(name = "Croesus Chest Price Overlay", desc = "Display each chests' profit for Dungeons and Kuudra.")
    @ConfigEditorBoolean
    @FeatureToggle
    var croesusAllChestsOverlay: Boolean = false

    @Expose
    @ConfigOption(
        name = "Croesus Chest Highlight",
        desc = "Highlights most profitable chest for Dungeons and Kuudra."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var croesusHighlight: Boolean = false

    @Suppress("Line Too Long")
    @Expose
    @ConfigOption(
        name = "Favorite Item Keybind",
        desc = "Press while Hovering an item to add/remove it from favorites which will Star chests containing this item in Croesus" +
            " and highlight the Item in Instance Chests."
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var keybind: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigLink(owner = InstanceChestProfitConfig::class, field = "croesusAllChestsOverlay")
    val croesusPosition: Position = Position(107, 141)
}
