package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class StarlynContestsConfig {

    @Expose
    @ConfigOption(name = "Agatha Coupon Prices", desc = "Help to identify profitable items to buy at Agatha's shop.")
    @ConfigEditorBoolean
    @FeatureToggle
    var agathaCouponProfitEnabled = true

    @Expose
    @ConfigLink(owner = StarlynContestsConfig::class, field = "agathaCouponProfitEnabled")
    val agathaCouponProfitPos: Position = Position(206, 158)

}
