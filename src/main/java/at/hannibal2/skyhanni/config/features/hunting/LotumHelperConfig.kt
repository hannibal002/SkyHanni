package at.hannibal2.skyhanni.config.features.hunting

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class LotumHelperConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Draw a green line to clicked Lotums on Lotus Atoll.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled = false

    @Expose
    @ConfigOption(name = "Highlight Lotums", desc = "Highlight all Lotums on Lotus Atoll.")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightLotums = false
}
