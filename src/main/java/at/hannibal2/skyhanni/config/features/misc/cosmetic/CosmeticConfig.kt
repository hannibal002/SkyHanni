package at.hannibal2.skyhanni.config.features.misc.cosmetic

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CosmeticConfig {
    @Expose
    @ConfigOption(name = "Following Line", desc = "")
    @Accordion
    var followingLine: FollowingLineConfig = FollowingLineConfig()

    @Expose
    @ConfigOption(name = "Arrow Trail", desc = "")
    @Accordion
    var arrowTrail: ArrowTrailConfig = ArrowTrailConfig()
}
