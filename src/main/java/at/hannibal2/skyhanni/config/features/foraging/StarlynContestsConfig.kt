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
    @ConfigOption(name = "Coupon Prices", desc = "Help to identify profitable items to buy at Starlyn Sister shops.")
    @ConfigEditorBoolean
    @FeatureToggle
    var starlynCouponProfitEnabled = true

    @Expose
    @ConfigOption(name = "Compact Results", desc = "Compacts the messages for your placement in a §dStarlyn Sister §7contest.")
    @SearchTag("Agatha Miria")
    @ConfigEditorBoolean
    @FeatureToggle
    var compactResults = false

    @Expose
    @ConfigOption(name = "Compact Personal Bests", desc = "Compact messages from log collection §dpersonal bests §7during contests.")
    @SearchTag("Agatha Miria")
    @ConfigEditorBoolean
    @FeatureToggle
    var compactPersonalBest = false

    @Expose
    @ConfigLink(owner = StarlynContestsConfig::class, field = "starlynCouponProfitEnabled")
    val starlynCouponProfitPos: Position = Position(206, 158)

}
