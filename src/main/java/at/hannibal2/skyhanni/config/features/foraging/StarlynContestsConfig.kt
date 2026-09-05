package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class StarlynContestsConfig {

    @Expose
    @ConfigOption(name = "Shop Profit", desc = "Helps to identify profitable items to buy at §dStarlyn Sister §fshops.")
    @ConfigEditorBoolean
    @FeatureToggle
    var starlynCouponProfitEnabled = true

    @Expose
    @ConfigOption(name = "Coupon Amount", desc = "Displays the number of §dStarlyn Sister §fcoupons you own while in their shops.")
    @ConfigEditorBoolean
    @FeatureToggle
    var starlynCouponAmount = true

    @Expose
    @ConfigOption(name = "Compact Results", desc = "Compacts the messages for your placement in a §dStarlyn Sister §fcontest.")
    @SearchTag("Agatha Miria")
    @ConfigEditorBoolean
    @FeatureToggle
    var compactResults = false

    @Expose
    @ConfigOption(name = "Compact Personal Bests", desc = "Compact messages from log collection §dPersonal Bests §fduring contests.")
    @SearchTag("Agatha Miria")
    @ConfigEditorBoolean
    @FeatureToggle
    var compactPersonalBest = false

    @Expose
    @ConfigLink(owner = StarlynContestsConfig::class, field = "starlynCouponProfitEnabled")
    val starlynCouponProfitPos: Position = Position(206, 158)

}
