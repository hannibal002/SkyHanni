package at.hannibal2.skyhanni.config.features.event

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class YearOfTheSealConfig {
    @Expose
    @ConfigOption(
        name = "Fishy Treat Profit",
        desc = "Shows what items to buy with your hard earned Fishy Treat.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var fishyTreatProfit: Boolean = true

    @Expose
    @ConfigLink(owner = YearOfTheSealConfig::class, field = "fishyTreatProfit")
    var fishyTreatProfitPosition: Position = Position(170, 150)
}
