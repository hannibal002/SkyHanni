package at.hannibal2.skyhanni.config.features.combat.end

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DpsConfig {
    @Expose
    @ConfigOption(
        name = "DPS Message",
        desc = "Shows your damage per second in chat after an End boss died."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var chatMessage: Boolean = false

    @Expose
    @ConfigOption(name = "Show Top Damager", desc = "Also show the DPS of the top damager.")
    @ConfigEditorBoolean
    var showTopDamager: Boolean = true
}
