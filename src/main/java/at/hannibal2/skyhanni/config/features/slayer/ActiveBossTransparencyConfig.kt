package at.hannibal2.skyhanni.config.features.slayer

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ActiveBossTransparencyConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Reduce the transparency of other mobs while fighting a slayer boss.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Transparency Strength",
        desc = "Adjust the level of transparency in percentages.",
    )
    @ConfigEditorSlider(minValue = 15f, maxValue = 70f, minStep = 1f)
    var transparencyLevel: Int = 35

    @Expose
    @ConfigOption(name = "Other Players", desc = "Also change the transparency for other players.")
    @ConfigEditorBoolean
    var applyToPlayers: Boolean = false
}
