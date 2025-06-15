package at.hannibal2.skyhanni.config.features.rift.motes

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MotesConfig {
    @Expose
    @ConfigOption(name = "Show Motes Price", desc = "Show the Motes NPC price in the item lore.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showPrice: Boolean = true

    @Expose
    @ConfigOption(name = "Burger Stacks", desc = "Set your McGrubber's burger stacks.")
    @ConfigEditorSlider(minStep = 1f, minValue = 0f, maxValue = 5f)
    var burgerStacks: Int = 0

    @Expose
    @ConfigOption(name = "Motes per Session", desc = "Show how many motes you got this session when leaving the rift.")
    @ConfigEditorBoolean
    @FeatureToggle
    var motesPerSession: Boolean = true

    @Expose
    @ConfigOption(name = "Inventory Value", desc = "")
    @Accordion
    val inventoryValue: RiftInventoryValueConfig = RiftInventoryValueConfig()
}
