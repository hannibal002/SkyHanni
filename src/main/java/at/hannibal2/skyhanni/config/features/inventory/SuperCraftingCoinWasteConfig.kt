package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.inventory.chocolatefactory.CFConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SuperCraftingCoinWasteConfig {
    @Expose
    @ConfigOption(name = "Warn about Super Crafting Coin Waste", desc = "Warns you when you can save more than Xm coins by insta buying the item and instant selling the materials.")
    @ConfigEditorBoolean
    @FeatureToggle
    val warnCoinWasteEnabled: Boolean = true

    @Expose
    @ConfigOption(name = "Minimum Amount", desc = "The minimum amount of coins (in millions) you must save (instant sell and insta buy " +
        "for wanted) to get a warning.")
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 50.0f, minStep = 0.1f)
    val warnCoinWaste: Double = 20.0

    @Expose
    @ConfigOption(name = "Minimum Amount (With Booster Cookie active)", desc = "The minimum amount of coins (in millions) you must save " +
        "(instant sell and insta buy for wanted) to get a warning while having Booster Cookie Buff.")
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 50.0f, minStep = 0.1f)
    val warnCoinWasteWithCookie: Double = 10.0

    @Expose
    @ConfigOption(name = "Minimum Amount if Max Resource Usage", desc = "Minimum amount of coins when compacting items due to space reasons.")
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 50.0f, minStep = 0.1f)
    val warnCoinWasteMaxResources: Double = 10.0

    @Expose
    @ConfigOption(name = "Minimum Amount if Max Resource Usage (With Booster Cookie active)", desc = "Minimum amount of coins when compacting items due to space reasons while having Booster Cookie Buff.")
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 50.0f, minStep = 0.1f)
    val warnCoinWasteMaxResourcesWithCookie: Double = 5.0

    @Expose
    @ConfigLink(owner = SuperCraftingCoinWasteConfig::class, field = "warnCoinWaste")
    val warnCoinWastePosition: Position = Position(300, 300)
}
