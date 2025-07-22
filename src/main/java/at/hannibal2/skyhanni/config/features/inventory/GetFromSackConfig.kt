package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GetFromSackConfig {
    @Expose
    @ConfigOption(
        name = "Queued GfS",
        desc = "If §e/gfs §7or §e/getfromsacks §7is used it queues up the commands so all items are guaranteed to be received."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var queuedGFS: Boolean = true

    @Expose
    @ConfigOption(
        name = "Bazaar GfS",
        desc = "If you don't have enough items in sack get a prompt to buy them from bazaar."
    )
    @ConfigEditorBoolean
    var bazaarGFS: Boolean = false

    @Expose
    @ConfigOption(
        name = "Super Craft GfS",
        desc = "Send a clickable message after supercrafting an item that grabs the item from your sacks when clicked."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var superCraftGFS: Boolean = true

    @Expose
    @ConfigOption(name = "Default Amount GfS", desc = "The default amount of items used when an amount isn't provided.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 64f, minStep = 1f)
    var defaultAmountGFS: Int = 1
}
