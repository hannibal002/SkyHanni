package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.OnlyModern
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
    @ConfigOption(name = "Compact Results", desc = "Compacts the messages for your placement in a §dStarlyn Sister §7contest.")
    @ConfigEditorBoolean
    @FeatureToggle
    @OnlyModern
    var compactResults = false

    @Expose
    @ConfigOption(name = "Compact Personal Bests", desc = "Compact messages from log collection §dpersonal bests §7during contests.")
    @ConfigEditorBoolean
    @FeatureToggle
    @OnlyModern
    var compactPersonalBest = false

    @Expose
    @ConfigLink(owner = StarlynContestsConfig::class, field = "agathaCouponProfitEnabled")
    val agathaCouponProfitPos: Position = Position(206, 158)

}
