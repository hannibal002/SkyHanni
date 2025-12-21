package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HideNotClickableConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Gray out items that are not clickable in the current inventory: Auction House, Bazaar, Accessory Bag, etc.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Protect Rarely Sold Items",
        desc = "Also gray out items that are not commonly sold to NPCs in NPC shop menus.\n" +
            "§eRequires main toggle to be enabled!",
    )
    @ConfigEditorBoolean
    var protectRarelySoldItems: Boolean = false

    @Expose
    @ConfigOption(name = "Block Clicks", desc = "Block the clicks on these items.")
    @ConfigEditorBoolean
    var itemsBlockClicks: Boolean = true

    @Expose
    @ConfigOption(name = "Opacity", desc = "How strong should the items be grayed out?")
    @ConfigEditorSlider(minValue = 0f, maxValue = 255f, minStep = 5f)
    var opacity: Int = 180

    @Expose
    @ConfigOption(
        name = "Bypass With Key",
        desc = "Add the ability to bypass not clickable items when holding the control/command key.",
    )
    @ConfigEditorBoolean
    var itemsBypass: Boolean = true

    @Expose
    @ConfigOption(name = "Green Line", desc = "Add green line around items that are clickable.")
    @ConfigEditorBoolean
    var itemsGreenLine: Boolean = true
}
