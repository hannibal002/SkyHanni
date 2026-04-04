package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

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
    @ConfigOption(name = "Transparency", desc = "How transparent should the items be?")
    @ConfigEditorSlider(minValue = 0f, maxValue = 255f, minStep = 5f)
    var transparency: Int = 180

    @Expose
    @ConfigOption(
        name = "Bypass With Key",
        desc = "Add the ability to bypass not clickable items when holding the Control key " +
            "(Command on macOS)",
    )
    @SearchTag("cmd ctrl")
    @ConfigEditorBoolean
    var itemsBypass: Boolean = true

    @Expose
    @ConfigOption(name = "Green Line", desc = "Add green line around items that are clickable.")
    @ConfigEditorBoolean
    var itemsGreenLine: Boolean = true

    @SkyHanniModule
    companion object {
        @HandleEvent
        fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
            val base = "inventory.hideNotClickable"
            event.move(126, "$base.opacity", "$base.transparency")
        }
    }
}
